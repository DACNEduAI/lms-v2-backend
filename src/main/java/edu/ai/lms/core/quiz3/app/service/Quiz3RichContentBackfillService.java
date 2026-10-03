package edu.ai.lms.core.quiz3.app.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import edu.ai.lms.core.quiz3.adapter.out.persistence.Quiz3RichContentAssetRepository;
import edu.ai.lms.core.quiz3.app.domain.richcontent.RichTextValue;
import edu.ai.lms.core.quiz3.app.service.Quiz3RichContentNormalizer.HistoricalDecision;
import lombok.RequiredArgsConstructor;

/** Explicit, audited and repeatable repair tool for Quiz3 content and immutable attempt snapshots. */
@Service
@RequiredArgsConstructor
public class Quiz3RichContentBackfillService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final Quiz3RichContentNormalizer normalizer;
    private final Quiz3RichContentAssetRepository assets;
    private final Quiz3RichContentUploadService uploads;

    public BackfillReport run(boolean apply, int requestedBatchSize) {
        int batchSize = Math.max(1, Math.min(requestedBatchSize, 1_000));
        UUID runId = UUID.randomUUID();
        List<Candidate> candidates = loadCandidates();
        Map<UUID, Set<String>> imageUrls = new LinkedHashMap<>();
        Map<HistoricalDecision, Integer> decisions = new EnumMap<>(HistoricalDecision.class);
        List<Change> changes = new ArrayList<>();
        int skipped = 0, unchanged = 0;

        for (Candidate candidate : candidates) {
            Set<String> allowed = imageUrls.computeIfAbsent(candidate.questionId(), questionId ->
                    uploads.publicUrls(assets.findByQuestionId(questionId), List.of()));
            var result = normalizer.inspectHistorical(candidate.content(), candidate.contentFormat(), allowed::contains);
            decisions.merge(result.decision(), 1, Integer::sum);
            if (!result.convertible()) {
                skipped++;
                continue;
            }
            RichTextValue value = result.value();
            if (value.content().equals(candidate.content()) && value.contentFormat().name().equals(candidate.contentFormat())) {
                unchanged++;
                continue;
            }
            changes.add(new Change(candidate, value));
        }

        int applied = 0, conflicts = 0;
        if (apply) {
            requireAuditTable();
            for (int start = 0; start < changes.size(); start += batchSize) {
                int end = Math.min(start + batchSize, changes.size());
                List<Change> batch = changes.subList(start, end);
                BatchResult result = transactions.execute(status -> applyBatch(runId, batch));
                if (result != null) {
                    applied += result.applied();
                    conflicts += result.conflicts();
                }
            }
        }
        return new BackfillReport(runId, !apply, candidates.size(), changes.size() + unchanged, applied,
                skipped, unchanged, conflicts, Map.copyOf(decisions));
    }

    public RestoreReport restore(UUID runId, int requestedBatchSize) {
        requireAuditTable();
        int batchSize = Math.max(1, Math.min(requestedBatchSize, 1_000));
        List<AuditEntry> entries = jdbc.query("""
                SELECT id,entity_type,field_name,question_id,answer_number,attempt_id,
                       before_content,before_format,after_content,after_format
                FROM edu.quiz3_rich_content_backfill_audit
                WHERE run_id=? AND restored_at IS NULL ORDER BY id DESC
                """, (rs,row) -> new AuditEntry(rs.getLong(1), EntityType.valueOf(rs.getString(2)), rs.getString(3),
                rs.getObject(4,UUID.class), (Long)rs.getObject(5), rs.getObject(6,UUID.class),
                rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10)), runId);
        int restored = 0, conflicts = 0;
        for (int start = 0; start < entries.size(); start += batchSize) {
            int end = Math.min(start + batchSize, entries.size());
            List<AuditEntry> batch = entries.subList(start, end);
            BatchResult result = transactions.execute(status -> restoreBatch(batch));
            if (result != null) {
                restored += result.applied();
                conflicts += result.conflicts();
            }
        }
        return new RestoreReport(runId, entries.size(), restored, conflicts);
    }

    private List<Candidate> loadCandidates() {
        List<Candidate> result = new ArrayList<>();
        jdbc.query("""
                SELECT DISTINCT q.id,q.content,q.content_format,q.explanation,q.explanation_format
                FROM edu.questions q JOIN edu.quiz3_assessment_variants v ON v.quiz_id=q.quiz_id
                ORDER BY q.id
                """, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
            UUID questionId = rs.getObject(1, UUID.class);
            result.add(new Candidate(EntityType.QUESTION, "content", questionId, null, null, rs.getString(2), rs.getString(3)));
            result.add(new Candidate(EntityType.QUESTION, "explanation", questionId, null, null, rs.getString(4), rs.getString(5)));
        });
        jdbc.query("""
                SELECT a.question_id,a.answer_number,a.content,a.content_format,a.explanation,a.explanation_format
                FROM edu.answers a JOIN edu.questions q ON q.id=a.question_id
                JOIN edu.quiz3_assessment_variants v ON v.quiz_id=q.quiz_id
                ORDER BY a.question_id,a.answer_number
                """, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
            UUID questionId = rs.getObject(1, UUID.class);
            long answerNumber = rs.getLong(2);
            result.add(new Candidate(EntityType.ANSWER, "content", questionId, answerNumber, null, rs.getString(3), rs.getString(4)));
            result.add(new Candidate(EntityType.ANSWER, "explanation", questionId, answerNumber, null, rs.getString(5), rs.getString(6)));
        });
        jdbc.query("""
                SELECT r.attempt_id,r.question_id,r.question_content,r.question_content_format,
                       r.student_answer_content,r.student_answer_content_format,
                       r.correct_answer_content,r.correct_answer_content_format
                FROM edu.attempt_records r JOIN edu.attempts a ON a.id=r.attempt_id
                JOIN edu.quiz3_assessment_variants v ON v.quiz_id=a.quiz_id
                ORDER BY r.attempt_id,r.interaction_order
                """, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
            UUID attemptId = rs.getObject(1, UUID.class), questionId = rs.getObject(2, UUID.class);
            result.add(new Candidate(EntityType.ATTEMPT_RECORD, "question_content", questionId, null, attemptId, rs.getString(3), rs.getString(4)));
            result.add(new Candidate(EntityType.ATTEMPT_RECORD, "student_answer_content", questionId, null, attemptId, rs.getString(5), rs.getString(6)));
            result.add(new Candidate(EntityType.ATTEMPT_RECORD, "correct_answer_content", questionId, null, attemptId, rs.getString(7), rs.getString(8)));
        });
        return result;
    }

    private BatchResult applyBatch(UUID runId, List<Change> changes) {
        int applied = 0, conflicts = 0;
        for (Change change : changes) {
            Candidate before = change.before();
            RichTextValue after = change.after();
            int updated = update(before, before.content(), before.contentFormat(), after.content(), after.contentFormat().name());
            if (updated != 1) {
                conflicts++;
                continue;
            }
            jdbc.update("""
                    INSERT INTO edu.quiz3_rich_content_backfill_audit
                        (run_id,entity_type,field_name,question_id,answer_number,attempt_id,
                         before_content,before_format,after_content,after_format)
                    VALUES (?,?,?,?,?,?,?,?,?,?)
                    """, runId, before.entityType().name(), before.fieldName(), before.questionId(), before.answerNumber(),
                    before.attemptId(), before.content(), before.contentFormat(), after.content(), after.contentFormat().name());
            applied++;
        }
        return new BatchResult(applied, conflicts);
    }

    private BatchResult restoreBatch(List<AuditEntry> entries) {
        int restored = 0, conflicts = 0;
        for (AuditEntry entry : entries) {
            Candidate key = new Candidate(entry.entityType(), entry.fieldName(), entry.questionId(), entry.answerNumber(),
                    entry.attemptId(), entry.afterContent(), entry.afterFormat());
            int updated = update(key, entry.afterContent(), entry.afterFormat(), entry.beforeContent(), entry.beforeFormat());
            if (updated != 1) {
                conflicts++;
                continue;
            }
            jdbc.update("UPDATE edu.quiz3_rich_content_backfill_audit SET restored_at=CURRENT_TIMESTAMP WHERE id=? AND restored_at IS NULL",
                    entry.id());
            restored++;
        }
        return new BatchResult(restored, conflicts);
    }

    private int update(Candidate candidate, String expectedContent, String expectedFormat,
            String replacementContent, String replacementFormat) {
        return switch (candidate.entityType()) {
            case QUESTION -> updateQuestion(candidate, expectedContent, expectedFormat, replacementContent, replacementFormat);
            case ANSWER -> updateAnswer(candidate, expectedContent, expectedFormat, replacementContent, replacementFormat);
            case ATTEMPT_RECORD -> updateAttempt(candidate, expectedContent, expectedFormat, replacementContent, replacementFormat);
        };
    }

    private int updateQuestion(Candidate value,String oldContent,String oldFormat,String newContent,String newFormat) {
        String sql = switch (value.fieldName()) {
            case "content" -> "UPDATE edu.questions SET content=?,content_format=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND content IS NOT DISTINCT FROM ? AND content_format IS NOT DISTINCT FROM ?";
            case "explanation" -> "UPDATE edu.questions SET explanation=?,explanation_format=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND explanation IS NOT DISTINCT FROM ? AND explanation_format IS NOT DISTINCT FROM ?";
            default -> throw new IllegalArgumentException("Unsupported question field: " + value.fieldName());
        };
        return jdbc.update(sql,newContent,newFormat,value.questionId(),oldContent,oldFormat);
    }

    private int updateAnswer(Candidate value,String oldContent,String oldFormat,String newContent,String newFormat) {
        String sql = switch (value.fieldName()) {
            case "content" -> "UPDATE edu.answers SET content=?,content_format=?,updated_at=CURRENT_TIMESTAMP WHERE question_id=? AND answer_number=? AND content IS NOT DISTINCT FROM ? AND content_format IS NOT DISTINCT FROM ?";
            case "explanation" -> "UPDATE edu.answers SET explanation=?,explanation_format=?,updated_at=CURRENT_TIMESTAMP WHERE question_id=? AND answer_number=? AND explanation IS NOT DISTINCT FROM ? AND explanation_format IS NOT DISTINCT FROM ?";
            default -> throw new IllegalArgumentException("Unsupported answer field: " + value.fieldName());
        };
        return jdbc.update(sql,newContent,newFormat,value.questionId(),value.answerNumber(),oldContent,oldFormat);
    }

    private int updateAttempt(Candidate value,String oldContent,String oldFormat,String newContent,String newFormat) {
        String sql = switch (value.fieldName()) {
            case "question_content" -> "UPDATE edu.attempt_records SET question_content=?,question_content_format=?,updated_at=CURRENT_TIMESTAMP WHERE attempt_id=? AND question_id=? AND question_content IS NOT DISTINCT FROM ? AND question_content_format IS NOT DISTINCT FROM ?";
            case "student_answer_content" -> "UPDATE edu.attempt_records SET student_answer_content=?,student_answer_content_format=?,updated_at=CURRENT_TIMESTAMP WHERE attempt_id=? AND question_id=? AND student_answer_content IS NOT DISTINCT FROM ? AND student_answer_content_format IS NOT DISTINCT FROM ?";
            case "correct_answer_content" -> "UPDATE edu.attempt_records SET correct_answer_content=?,correct_answer_content_format=?,updated_at=CURRENT_TIMESTAMP WHERE attempt_id=? AND question_id=? AND correct_answer_content IS NOT DISTINCT FROM ? AND correct_answer_content_format IS NOT DISTINCT FROM ?";
            default -> throw new IllegalArgumentException("Unsupported attempt field: " + value.fieldName());
        };
        return jdbc.update(sql,newContent,newFormat,value.attemptId(),value.questionId(),oldContent,oldFormat);
    }

    private void requireAuditTable() {
        Boolean present = jdbc.queryForObject("SELECT to_regclass('edu.quiz3_rich_content_backfill_audit') IS NOT NULL", Boolean.class);
        if (!Boolean.TRUE.equals(present)) {
            throw new IllegalStateException("Apply V20261002_01__quiz3_rich_content_backfill_audit.sql before apply or restore mode");
        }
    }

    private enum EntityType { QUESTION, ANSWER, ATTEMPT_RECORD }
    private record Candidate(EntityType entityType,String fieldName,UUID questionId,Long answerNumber,UUID attemptId,
            String content,String contentFormat) { }
    private record Change(Candidate before,RichTextValue after) { }
    private record AuditEntry(long id,EntityType entityType,String fieldName,UUID questionId,Long answerNumber,UUID attemptId,
            String beforeContent,String beforeFormat,String afterContent,String afterFormat) { }
    private record BatchResult(int applied,int conflicts) { }

    public record BackfillReport(UUID runId,boolean dryRun,int inspected,int convertible,int applied,int skipped,
            int unchanged,int conflicts,Map<HistoricalDecision,Integer> decisions) { }
    public record RestoreReport(UUID runId,int inspected,int restored,int conflicts) { }
}
