package edu.ai.lms.core.quiz.adapter.out.persistence;

import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AnswerView;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.BankPlan;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.BankPlanCell;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.BatchCellView;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.BatchView;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.DraftReviewItem;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.DraftReviewView;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.GeneratedDraft;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.PublishResult;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantQuestionView;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantView;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AssessmentView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.LifecycleStatus;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AssessmentWorkflowRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public BankPlan bankPlan(AssessmentView assessment) {
        if (assessment.examMatrixId() == null) throw new IllegalStateException("Select an Exam Matrix first");
        List<BankPlanCell> cells = jdbc.query("""
                SELECT r.id AS matrix_row_id, c.custom_learning_level_id, r.chapter_id,
                       ch.title AS chapter, r.topic, l.code AS level_code, l.name AS level_name,
                       c.question_count
                FROM edu.exam_matrix_row_learning_level_counts c
                JOIN edu.exam_matrix_rows r ON r.id = c.exam_matrix_row_id
                JOIN edu.custom_learning_levels l ON l.id = c.custom_learning_level_id
                LEFT JOIN edu.chapters ch ON ch.chapter_id = r.chapter_id
                WHERE r.matrix_id = :matrixId AND c.question_type = 'MULTIPLE_CHOICE'
                  AND c.question_count > 0
                ORDER BY r.order_index, l.sort_order, r.id, l.id
                """, Map.of("matrixId", assessment.examMatrixId()), (rs, rowNum) -> buildCell(assessment, rs));
        int required = cells.stream().mapToInt(BankPlanCell::required).sum();
        int validated = cells.stream().mapToInt(BankPlanCell::validated).sum();
        int recent = cells.stream().mapToInt(BankPlanCell::recentlyUsed).sum();
        int reserved = cells.stream().mapToInt(BankPlanCell::reserved).sum();
        int available = cells.stream().mapToInt(BankPlanCell::available).sum();
        int missing = cells.stream().mapToInt(BankPlanCell::missing).sum();
        return new BankPlan(assessment.id(), assessment.examMatrixId(), assessment.allowRecentlyUsedQuestions(),
                required, validated, recent, reserved, available, missing, cells);
    }

    private BankPlanCell buildCell(AssessmentView assessment, ResultSet rs) throws SQLException {
        UUID rowId = rs.getObject("matrix_row_id", UUID.class);
        UUID levelId = rs.getObject("custom_learning_level_id", UUID.class);
        UUID chapterId = rs.getObject("chapter_id", UUID.class);
        String levelCode = rs.getString("level_code");
        List<UUID> outcomeIds = cellOutcomeIds(rowId, levelId);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("courseId", assessment.courseId()).addValue("chapterId", chapterId)
                .addValue("levelId", levelId).addValue("levelCode", levelCode)
                .addValue("outcomeIds", outcomeIds.isEmpty() ? List.of(UUID.randomUUID()) : outcomeIds)
                .addValue("outcomeCount", outcomeIds.size());
        List<Map<String, Object>> candidates = jdbc.queryForList("""
                SELECT q.id, q.recently_used,
                       EXISTS (SELECT 1 FROM edu.question_bank_allocations a
                               WHERE a.question_bank_question_id = q.id AND a.status = 'RESERVED') AS reserved
                FROM edu.question_bank_questions q
                JOIN edu.cognitive_levels cl ON cl.id = q.cognitive_level_id
                WHERE q.course_id = :courseId AND q.question_type = 'MULTIPLE_CHOICE'
                  AND (:chapterId IS NULL OR q.chapter_id = :chapterId)
                  AND (q.custom_learning_level_id = :levelId OR lower(cl.code) = lower(:levelCode)
                       OR cl.sort_order = (SELECT sort_order FROM edu.custom_learning_levels WHERE id = :levelId))
                  AND (:outcomeCount = 0 OR (SELECT COUNT(DISTINCT qlo.learning_outcome_id)
                       FROM edu.question_bank_question_learning_outcomes qlo
                       WHERE qlo.question_bank_question_id = q.id
                         AND qlo.learning_outcome_id IN (:outcomeIds)) = :outcomeCount)
                """, parameters);
        int validated = candidates.size();
        int recent = (int) candidates.stream().filter(row -> Boolean.TRUE.equals(row.get("recently_used"))).count();
        int reserved = (int) candidates.stream().filter(row -> Boolean.TRUE.equals(row.get("reserved"))).count();
        int available = (int) candidates.stream().filter(row -> !Boolean.TRUE.equals(row.get("reserved")))
                .filter(row -> assessment.allowRecentlyUsedQuestions() || !Boolean.TRUE.equals(row.get("recently_used")))
                .count();
        int required = rs.getInt("question_count");
        return new BankPlanCell(rowId, levelId, chapterId, rs.getString("chapter"), rs.getString("topic"),
                levelCode, rs.getString("level_name"), outcomeIds, required, validated, recent, reserved,
                available, Math.max(0, required - available));
    }

    private List<UUID> cellOutcomeIds(UUID rowId, UUID levelId) {
        return jdbc.query("""
                SELECT rlo.learning_outcome_id
                FROM edu.exam_matrix_row_learning_outcomes rlo
                JOIN edu.custom_learning_level_learning_outcomes llo
                  ON llo.learning_outcome_id = rlo.learning_outcome_id
                WHERE rlo.exam_matrix_row_id = :rowId AND llo.custom_learning_level_id = :levelId
                  AND rlo.is_selected = true AND llo.is_selected = true
                ORDER BY rlo.learning_outcome_id
                """, Map.of("rowId", rowId, "levelId", levelId),
                (rs, rowNum) -> rs.getObject("learning_outcome_id", UUID.class));
    }

    public BatchView createBatch(AssessmentView assessment, BankPlan plan, UUID actorId, String requestSnapshot) {
        UUID batchId = UUID.randomUUID();
        String idempotencyKey = assessment.id() + ":" + batchId;
        List<BankPlanCell> missingCells = plan.cells().stream().filter(cell -> cell.missing() > 0).toList();
        jdbc.update("""
                INSERT INTO edu.assessment_generation_batches
                    (id, assessment_id, status, option_count, total_cells, request_snapshot,
                     idempotency_key, created_by)
                VALUES (:id, :assessmentId, :status, :optionCount, :totalCells,
                        CAST(:requestSnapshot AS jsonb), :idempotencyKey, :actorId)
                """, new MapSqlParameterSource().addValue("id", batchId).addValue("assessmentId", assessment.id())
                .addValue("status", missingCells.isEmpty() ? "COMPLETED" : "QUEUED")
                .addValue("optionCount", assessment.optionCount()).addValue("totalCells", missingCells.size())
                .addValue("requestSnapshot", requestSnapshot).addValue("idempotencyKey", idempotencyKey)
                .addValue("actorId", actorId));
        for (BankPlanCell cell : missingCells) {
            jdbc.update("""
                    INSERT INTO edu.assessment_generation_batch_items
                        (id, batch_id, matrix_row_id, custom_learning_level_id, required_count)
                    VALUES (:id, :batchId, :matrixRowId, :levelId, :requiredCount)
                    """, Map.of("id", UUID.randomUUID(), "batchId", batchId,
                    "matrixRowId", cell.matrixRowId(), "levelId", cell.customLearningLevelId(),
                    "requiredCount", cell.missing()));
        }
        if (missingCells.isEmpty()) {
            jdbc.update("""
                    UPDATE edu.assessments SET lifecycle_status = 'REVIEW_PENDING',
                        current_step = GREATEST(current_step, 8), version = version + 1,
                        updated_at = CURRENT_TIMESTAMP WHERE id = :assessmentId
                    """, Map.of("assessmentId", assessment.id()));
        }
        return getBatch(assessment.courseId(), assessment.id(), batchId);
    }

    public BatchView getBatch(UUID courseId, UUID assessmentId, UUID batchId) {
        BatchView base = jdbc.query("""
                SELECT b.id, b.assessment_id, b.status, b.option_count, b.total_cells,
                       b.completed_cells, b.generated_questions, b.error_message,
                       b.created_at, b.completed_at
                FROM edu.assessment_generation_batches b
                JOIN edu.assessments a ON a.id = b.assessment_id
                WHERE a.course_id = :courseId AND b.assessment_id = :assessmentId AND b.id = :batchId
                """, Map.of("courseId", courseId, "assessmentId", assessmentId, "batchId", batchId),
                (rs, rowNum) -> new BatchView(rs.getObject("id", UUID.class),
                        rs.getObject("assessment_id", UUID.class), rs.getString("status"),
                        rs.getInt("option_count"), rs.getInt("total_cells"), rs.getInt("completed_cells"),
                        rs.getInt("generated_questions"), rs.getString("error_message"),
                        rs.getObject("created_at", LocalDateTime.class),
                        rs.getObject("completed_at", LocalDateTime.class), List.of()))
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Generation batch was not found"));
        List<BatchCellView> cells = batchCells(batchId);
        return new BatchView(base.id(), base.assessmentId(), base.status(), base.optionCount(), base.totalCells(),
                base.completedCells(), base.generatedQuestions(), base.errorMessage(), base.createdAt(),
                base.completedAt(), cells);
    }

    public List<BatchView> listBatches(UUID courseId, UUID assessmentId) {
        return jdbc.query("""
                SELECT b.id FROM edu.assessment_generation_batches b
                JOIN edu.assessments a ON a.id = b.assessment_id
                WHERE a.course_id = :courseId AND b.assessment_id = :assessmentId
                ORDER BY b.created_at DESC, b.id DESC
                """, Map.of("courseId", courseId, "assessmentId", assessmentId),
                (rs, rowNum) -> rs.getObject("id", UUID.class)).stream()
                .map(batchId -> getBatch(courseId, assessmentId, batchId))
                .toList();
    }

    public BatchView activeBatch(UUID courseId, UUID assessmentId) {
        return jdbc.query("""
                SELECT b.id FROM edu.assessment_generation_batches b
                JOIN edu.assessments a ON a.id = b.assessment_id
                WHERE a.course_id = :courseId AND b.assessment_id = :assessmentId
                  AND b.status IN ('QUEUED', 'PROCESSING')
                ORDER BY b.created_at DESC, b.id DESC LIMIT 1
                """, Map.of("courseId", courseId, "assessmentId", assessmentId),
                (rs, rowNum) -> rs.getObject("id", UUID.class)).stream()
                .findFirst().map(batchId -> getBatch(courseId, assessmentId, batchId)).orElse(null);
    }

    public String batchRequestSnapshot(UUID batchId) {
        return jdbc.queryForObject("SELECT request_snapshot::text FROM edu.assessment_generation_batches WHERE id = :batchId",
                Map.of("batchId", batchId), String.class);
    }

    public void markBatchProcessing(UUID batchId) {
        jdbc.update("""
                UPDATE edu.assessment_generation_batches SET status = 'PROCESSING', updated_at = CURRENT_TIMESTAMP
                WHERE id = :batchId AND status = 'QUEUED'
                """, Map.of("batchId", batchId));
        jdbc.update("""
                UPDATE edu.assessment_generation_batch_items SET status = 'PROCESSING', updated_at = CURRENT_TIMESTAMP
                WHERE batch_id = :batchId AND status = 'QUEUED'
                """, Map.of("batchId", batchId));
    }

    public void failBatch(UUID batchId, String message) {
        jdbc.update("""
                UPDATE edu.assessment_generation_batches SET status = 'FAILED', error_message = :message,
                    completed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = :batchId
                """, Map.of("batchId", batchId, "message", message));
        jdbc.update("""
                UPDATE edu.assessment_generation_batch_items SET status = 'FAILED', error_message = :message,
                    updated_at = CURRENT_TIMESTAMP WHERE batch_id = :batchId AND status <> 'COMPLETED'
                """, Map.of("batchId", batchId, "message", message));
    }

    public String prepareCellRetry(UUID courseId, UUID assessmentId, UUID batchId, UUID cellId) {
        Map<String, Object> cell = jdbc.queryForMap("""
                SELECT i.matrix_row_id, i.custom_learning_level_id, i.status
                FROM edu.assessment_generation_batch_items i
                JOIN edu.assessment_generation_batches b ON b.id = i.batch_id
                JOIN edu.assessments a ON a.id = b.assessment_id
                WHERE a.course_id = :courseId AND a.id = :assessmentId
                  AND b.id = :batchId AND i.id = :cellId
                """, Map.of("courseId", courseId, "assessmentId", assessmentId,
                "batchId", batchId, "cellId", cellId));
        if (!"FAILED".equals(String.valueOf(cell.get("status")))) {
            throw new IllegalStateException("Only a failed Matrix cell can be retried");
        }
        jdbc.update("""
                UPDATE edu.assessment_generation_batch_items
                SET status = 'PROCESSING', error_message = NULL, retry_count = retry_count + 1,
                    updated_at = CURRENT_TIMESTAMP WHERE id = :cellId
                """, Map.of("cellId", cellId));
        jdbc.update("""
                UPDATE edu.assessment_generation_batches
                SET status = 'PROCESSING', error_message = NULL, completed_at = NULL,
                    updated_at = CURRENT_TIMESTAMP WHERE id = :batchId
                """, Map.of("batchId", batchId));
        try {
            ObjectNode snapshot = (ObjectNode) objectMapper.readTree(batchRequestSnapshot(batchId));
            ArrayNode selected = objectMapper.createArrayNode();
            snapshot.withArray("cells").forEach(node -> {
                if (String.valueOf(cell.get("matrix_row_id")).equals(node.path("matrixRowId").asText())
                        && String.valueOf(cell.get("custom_learning_level_id"))
                                .equals(node.path("customLearningLevelId").asText())) {
                    selected.add(node.deepCopy());
                }
            });
            if (selected.isEmpty()) throw new IllegalStateException("The failed cell is missing from the request snapshot");
            snapshot.set("cells", selected);
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to prepare the Matrix cell retry", ex);
        }
    }

    @Transactional
    public void persistGeneratedDrafts(UUID batchId, List<GeneratedDraft> drafts) {
        Map<String, Object> batch = jdbc.queryForMap("""
                SELECT b.assessment_id, a.course_id, a.created_by
                FROM edu.assessment_generation_batches b JOIN edu.assessments a ON a.id = b.assessment_id
                WHERE b.id = :batchId FOR UPDATE
                """, Map.of("batchId", batchId));
        UUID assessmentId = (UUID) batch.get("assessment_id");
        UUID courseId = (UUID) batch.get("course_id");
        UUID createdBy = (UUID) batch.get("created_by");
        List<BatchCellView> expected = batchCells(batchId);
        Map<String, BatchCellView> expectedByKey = new LinkedHashMap<>();
        expected.forEach(cell -> expectedByKey.put(cell.matrixRowId() + "|" + cell.customLearningLevelId(), cell));
        List<GeneratedDraft> acceptedDrafts = new ArrayList<>();
        Map<String, Integer> actual = new LinkedHashMap<>();
        for (GeneratedDraft draft : drafts) {
            validateGeneratedDraft(draft);
            String cellKey = draft.matrixRowId() + "|" + draft.customLearningLevelId();
            BatchCellView cell = expectedByKey.get(cellKey);
            if (cell == null) throw new IllegalStateException("AI returned a Matrix cell outside the batch request");
            if ("COMPLETED".equals(cell.status())) continue;
            actual.merge(cellKey, 1, Integer::sum);
            acceptedDrafts.add(draft);
        }
        for (BatchCellView cell : expected) {
            if (!"PROCESSING".equals(cell.status())) continue;
            int count = actual.getOrDefault(cell.matrixRowId() + "|" + cell.customLearningLevelId(), 0);
            if (count != cell.requiredCount()) {
                throw new IllegalStateException("AI result count does not match Matrix cell "
                        + cell.matrixRowId() + "/" + cell.customLearningLevelId());
            }
        }
        if (acceptedDrafts.isEmpty()) return;
        for (GeneratedDraft draft : acceptedDrafts) {
            UUID draftId = UUID.randomUUID();
            UUID chapterId = jdbc.queryForObject("SELECT chapter_id FROM edu.exam_matrix_rows WHERE id = :rowId",
                    Map.of("rowId", draft.matrixRowId()), UUID.class);
            UUID cognitiveLevelId = jdbc.queryForObject("""
                    SELECT COALESCE(
                        (SELECT c.id FROM edu.cognitive_levels c JOIN edu.custom_learning_levels l
                         ON lower(c.code) = lower(l.code) WHERE l.id = :levelId LIMIT 1),
                        (SELECT c.id FROM edu.cognitive_levels c JOIN edu.custom_learning_levels l
                         ON c.sort_order = l.sort_order WHERE l.id = :levelId LIMIT 1))
                    """, Map.of("levelId", draft.customLearningLevelId()), UUID.class);
            jdbc.update("""
                    INSERT INTO edu.question_drafts
                        (id, course_id, chapter_id, cognitive_level_id, content, question_type,
                         source_type, created_by, assessment_id, generation_batch_id,
                         matrix_row_id, custom_learning_level_id, review_status)
                    VALUES (:id, :courseId, :chapterId, :cognitiveLevelId, :content, 'MULTIPLE_CHOICE',
                            'AI_GENERATED', :createdBy, :assessmentId, :batchId, :matrixRowId, :levelId,
                            'NOT_VALIDATED')
                    """, new MapSqlParameterSource().addValue("id", draftId).addValue("courseId", courseId)
                    .addValue("chapterId", chapterId).addValue("cognitiveLevelId", cognitiveLevelId)
                    .addValue("content", draft.content()).addValue("createdBy", createdBy)
                    .addValue("assessmentId", assessmentId).addValue("batchId", batchId)
                    .addValue("matrixRowId", draft.matrixRowId()).addValue("levelId", draft.customLearningLevelId()));
            for (int i = 0; i < draft.answers().size(); i++) {
                var answer = draft.answers().get(i);
                jdbc.update("""
                        INSERT INTO edu.question_draft_answers
                            (id, question_draft_id, content, is_correct, explanation, order_index)
                        VALUES (:id, :draftId, :content, :correct, :explanation, :orderIndex)
                        """, new MapSqlParameterSource().addValue("id", UUID.randomUUID()).addValue("draftId", draftId)
                        .addValue("content", answer.content()).addValue("correct", answer.correct())
                        .addValue("explanation", answer.explanation()).addValue("orderIndex", i));
            }
            for (UUID outcomeId : draft.learningOutcomeIds()) {
                jdbc.update("""
                        INSERT INTO edu.question_draft_learning_outcomes (question_draft_id, learning_outcome_id)
                        VALUES (:draftId, :outcomeId)
                        """, Map.of("draftId", draftId, "outcomeId", outcomeId));
            }
        }
        for (String cellKey : actual.keySet()) {
            BatchCellView cell = expectedByKey.get(cellKey);
            jdbc.update("""
                    UPDATE edu.assessment_generation_batch_items
                    SET generated_count = required_count, status = 'COMPLETED', updated_at = CURRENT_TIMESTAMP
                    WHERE id = :id
                    """, Map.of("id", cell.id()));
        }
        Integer completedCells = jdbc.queryForObject("""
                SELECT COUNT(*) FROM edu.assessment_generation_batch_items
                WHERE batch_id = :batchId AND status = 'COMPLETED'
                """, Map.of("batchId", batchId), Integer.class);
        Integer totalCells = jdbc.queryForObject("""
                SELECT COUNT(*) FROM edu.assessment_generation_batch_items WHERE batch_id = :batchId
                """, Map.of("batchId", batchId), Integer.class);
        Integer generatedQuestions = jdbc.queryForObject("""
                SELECT COALESCE(SUM(generated_count), 0) FROM edu.assessment_generation_batch_items
                WHERE batch_id = :batchId
                """, Map.of("batchId", batchId), Integer.class);
        boolean completed = completedCells != null && completedCells.equals(totalCells);
        jdbc.update("""
                UPDATE edu.assessment_generation_batches
                SET status = :status, completed_cells = :completedCells,
                    generated_questions = :generatedQuestions,
                    completed_at = CASE WHEN :completed THEN CURRENT_TIMESTAMP ELSE NULL END,
                    updated_at = CURRENT_TIMESTAMP WHERE id = :batchId
                """, Map.of("batchId", batchId, "status", completed ? "COMPLETED" : "PARTIAL",
                "completedCells", completedCells == null ? 0 : completedCells,
                "generatedQuestions", generatedQuestions == null ? 0 : generatedQuestions,
                "completed", completed));
        jdbc.update("""
                UPDATE edu.assessments SET lifecycle_status = 'REVIEW_PENDING', current_step = 7,
                    version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :assessmentId
                """, Map.of("assessmentId", assessmentId));
    }

    public DraftReviewView review(UUID assessmentId) {
        List<DraftReviewItem> notValidated = draftItems(assessmentId, "NOT_VALIDATED");
        List<DraftReviewItem> rejected = draftItems(assessmentId, "REJECTED");
        List<DraftReviewItem> validated = bankItems(assessmentId);
        return new DraftReviewView(assessmentId, notValidated, validated, rejected);
    }

    public void rejectDraft(UUID assessmentId, UUID draftId, long version, UUID actorId, String reason) {
        int updated = jdbc.update("""
                UPDATE edu.question_drafts SET review_status = 'REJECTED', rejected_by = :actorId,
                    rejected_at = CURRENT_TIMESTAMP, rejection_reason = :reason,
                    version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = :draftId AND assessment_id = :assessmentId
                  AND review_status = 'NOT_VALIDATED' AND version = :version
                """, Map.of("actorId", actorId, "reason", reason, "draftId", draftId,
                "assessmentId", assessmentId, "version", version));
        if (updated != 1) throw new IllegalStateException("Draft changed before rejection");
    }

    public DraftProvenance draftProvenance(UUID assessmentId, UUID draftId) {
        return jdbc.query("""
                SELECT generation_batch_id, matrix_row_id, custom_learning_level_id
                FROM edu.question_drafts WHERE id = :draftId AND assessment_id = :assessmentId
                  AND review_status = 'NOT_VALIDATED'
                """, Map.of("draftId", draftId, "assessmentId", assessmentId),
                (rs, rowNum) -> new DraftProvenance(rs.getObject("generation_batch_id", UUID.class),
                        rs.getObject("matrix_row_id", UUID.class),
                        rs.getObject("custom_learning_level_id", UUID.class)))
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Draft is not reviewable"));
    }

    public void attachBankProvenance(UUID assessmentId, UUID originDraftId, DraftProvenance provenance) {
        jdbc.update("""
                UPDATE edu.question_bank_questions
                SET assessment_id = :assessmentId, generation_batch_id = :batchId,
                    matrix_row_id = :matrixRowId, custom_learning_level_id = :levelId
                WHERE origin_draft_id = :originDraftId
                """, new MapSqlParameterSource().addValue("assessmentId", assessmentId)
                .addValue("batchId", provenance.batchId()).addValue("matrixRowId", provenance.matrixRowId())
                .addValue("levelId", provenance.customLearningLevelId()).addValue("originDraftId", originDraftId));
    }

    public int resetRecentlyUsed(UUID courseId, List<UUID> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return 0;
        return jdbc.update("""
                UPDATE edu.question_bank_questions SET recently_used = false, updated_at = CURRENT_TIMESTAMP
                WHERE course_id = :courseId AND id IN (:ids)
                """, new MapSqlParameterSource().addValue("courseId", courseId).addValue("ids", questionIds));
    }

    @Transactional
    public List<VariantView> createVariants(AssessmentView assessment, BankPlan plan) {
        Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM edu.assessment_variants WHERE assessment_id = :id",
                Map.of("id", assessment.id()), Integer.class);
        if (existing != null && existing > 0) throw new IllegalStateException("Variants already exist for this assessment");
        if (plan.missing() > 0) throw new IllegalStateException("Question Bank still has missing Matrix cells");
        List<UUID> selected = new ArrayList<>();
        Set<UUID> selectedSet = new HashSet<>();
        for (BankPlanCell cell : plan.cells()) {
            List<UUID> candidates = candidateQuestionIds(assessment, cell, selectedSet, cell.required());
            if (candidates.size() != cell.required()) throw new IllegalStateException("Question Bank capacity changed during allocation");
            selected.addAll(candidates);
            selectedSet.addAll(candidates);
        }
        for (UUID bankId : selected) {
            try {
                jdbc.update("""
                        INSERT INTO edu.question_bank_allocations
                            (id, question_bank_question_id, assessment_id, status)
                        VALUES (:id, :bankId, :assessmentId, 'RESERVED')
                        """, Map.of("id", UUID.randomUUID(), "bankId", bankId, "assessmentId", assessment.id()));
            } catch (DuplicateKeyException ex) {
                throw new IllegalStateException("A selected question was reserved by another assessment", ex);
            }
        }
        for (int variantIndex = 0; variantIndex < assessment.variantCount(); variantIndex++) {
            UUID quizId = UUID.randomUUID();
            UUID variantId = UUID.randomUUID();
            String code = "%03d".formatted(variantIndex + 1);
            long seed = assessment.id().getMostSignificantBits() ^ assessment.id().getLeastSignificantBits() ^ variantIndex;
            jdbc.update("""
                    INSERT INTO edu.quizzes
                        (id, quiz_title, description, unlimited, time_limit_minutes, pass_score,
                         start_time, due_time, attempt_limited, course_id, teacher_id, display,
                         lifecycle_status, source_type)
                    VALUES (:id, :title, :description, :unlimited, :timeLimitMinutes, :passScore,
                            :startTime, :dueTime, :attemptLimited, :courseId, :teacherId, false,
                            'DRAFT', 'CLONED')
                    """, new MapSqlParameterSource().addValue("id", quizId)
                    .addValue("title", assessment.title() + " - " + code).addValue("description", assessment.description())
                    .addValue("unlimited", assessment.unlimited()).addValue("timeLimitMinutes", assessment.timeLimitMinutes())
                    .addValue("passScore", assessment.passScore()).addValue("startTime", assessment.startTime())
                    .addValue("dueTime", assessment.dueTime()).addValue("attemptLimited", assessment.attemptLimited())
                    .addValue("courseId", assessment.courseId()).addValue("teacherId", assessment.createdBy()));
            jdbc.update("""
                    INSERT INTO edu.assessment_variants
                        (id, assessment_id, quiz_id, variant_code, order_index, shuffle_seed)
                    VALUES (:id, :assessmentId, :quizId, :code, :orderIndex, :seed)
                    """, Map.of("id", variantId, "assessmentId", assessment.id(), "quizId", quizId,
                    "code", code, "orderIndex", variantIndex, "seed", seed));
            List<UUID> shuffled = new ArrayList<>(selected);
            Collections.shuffle(shuffled, new Random(seed));
            for (int questionIndex = 0; questionIndex < shuffled.size(); questionIndex++) {
                copyBankQuestionToQuiz(shuffled.get(questionIndex), quizId, questionIndex, seed);
            }
        }
        jdbc.update("""
                UPDATE edu.assessments SET lifecycle_status = 'READY_TO_PUBLISH', current_step = 9,
                    version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :id
                """, Map.of("id", assessment.id()));
        return variants(assessment.id());
    }

    public List<VariantView> variants(UUID assessmentId) {
        return jdbc.query("""
                SELECT id, quiz_id, variant_code, order_index, shuffle_seed, coverage_status,
                       override_reason, override_by, override_at
                FROM edu.assessment_variants WHERE assessment_id = :assessmentId ORDER BY order_index
                """, Map.of("assessmentId", assessmentId), (rs, rowNum) -> new VariantView(
                rs.getObject("id", UUID.class), rs.getObject("quiz_id", UUID.class), rs.getString("variant_code"),
                rs.getInt("order_index"), rs.getLong("shuffle_seed"), rs.getString("coverage_status"),
                rs.getString("override_reason"), rs.getObject("override_by", UUID.class),
                rs.getObject("override_at", LocalDateTime.class),
                variantQuestions(rs.getObject("quiz_id", UUID.class))));
    }

    public String recalculateCoverage(AssessmentView assessment, UUID variantId) {
        UUID quizId = variantQuiz(assessment.id(), variantId);
        List<Map<String, Object>> differences = new ArrayList<>();
        for (BankPlanCell cell : bankPlan(assessment).cells()) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("quizId", quizId).addValue("chapterId", cell.chapterId())
                    .addValue("levelId", cell.customLearningLevelId()).addValue("levelCode", cell.levelCode())
                    .addValue("outcomeIds", cell.learningOutcomeIds().isEmpty() ? List.of(UUID.randomUUID()) : cell.learningOutcomeIds())
                    .addValue("outcomeCount", cell.learningOutcomeIds().size());
            Integer actual = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM edu.questions q
                    LEFT JOIN edu.cognitive_levels cl ON cl.id = q.cognitive_level_id
                    WHERE q.quiz_id = :quizId AND q.type = 'MULTIPLE_CHOICE'
                      AND (:chapterId IS NULL OR q.chapter_id = :chapterId)
                      AND (cl.code IS NULL OR lower(cl.code) = lower(:levelCode)
                           OR cl.sort_order = (SELECT sort_order FROM edu.custom_learning_levels WHERE id = :levelId))
                      AND (:outcomeCount = 0 OR (SELECT COUNT(DISTINCT qlo.learning_outcome_id)
                           FROM edu.question_learning_outcomes qlo WHERE qlo.question_id = q.id
                             AND qlo.learning_outcome_id IN (:outcomeIds)) = :outcomeCount)
                    """, params, Integer.class);
            if (actual == null || actual != cell.required()) {
                differences.add(Map.of("matrixRowId", cell.matrixRowId(), "customLearningLevelId",
                        cell.customLearningLevelId(), "required", cell.required(), "actual", actual == null ? 0 : actual));
            }
        }
        String status = differences.isEmpty() ? "COMPLIANT" : "DRIFTED";
        String snapshot = toJson(Map.of("status", status, "differences", differences));
        jdbc.update("""
                UPDATE edu.assessment_variants SET coverage_status = :status,
                    coverage_snapshot = CAST(:snapshot AS jsonb), updated_at = CURRENT_TIMESTAMP
                WHERE id = :variantId AND assessment_id = :assessmentId
                """, Map.of("status", status, "snapshot", snapshot, "variantId", variantId,
                "assessmentId", assessment.id()));
        return status;
    }

    public void setOverride(UUID assessmentId, UUID variantId, UUID actorId, String reason) {
        int updated = jdbc.update("""
                UPDATE edu.assessment_variants SET override_reason = :reason, override_by = :actorId,
                    override_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = :variantId AND assessment_id = :assessmentId AND coverage_status = 'DRIFTED'
                """, Map.of("reason", reason, "actorId", actorId, "variantId", variantId,
                "assessmentId", assessmentId));
        if (updated != 1) throw new IllegalStateException("Only a drifted variant can receive an override reason");
    }

    @Transactional
    public PublishResult publish(AssessmentView assessment, UUID actorId) {
        List<VariantView> variants = variants(assessment.id());
        if (variants.size() != assessment.variantCount()) throw new IllegalStateException("Assessment variants are incomplete");
        for (VariantView variant : variants) {
            validateVariantStructure(variant.quizId(), assessment.optionCount());
            String status = recalculateCoverage(assessment, variant.id());
            if ("DRIFTED".equals(status)) {
                String reason = jdbc.queryForObject("SELECT override_reason FROM edu.assessment_variants WHERE id = :id",
                        Map.of("id", variant.id()), String.class);
                if (reason == null || reason.isBlank()) throw new IllegalStateException("Every drifted variant needs an override reason");
            }
        }
        LocalDateTime publishedAt = LocalDateTime.now();
        int publishedVariants = jdbc.update("""
                UPDATE edu.quizzes q SET lifecycle_status = 'PUBLISHED', display = false,
                    published_by = :actorId, published_at = :publishedAt,
                    version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE q.id IN (SELECT quiz_id FROM edu.assessment_variants WHERE assessment_id = :assessmentId)
                  AND q.lifecycle_status = 'DRAFT'
                """, Map.of("actorId", actorId, "publishedAt", publishedAt, "assessmentId", assessment.id()));
        if (publishedVariants != variants.size()) throw new IllegalStateException("One or more variants changed before publish");
        List<UUID> bankIds = jdbc.query("""
                SELECT DISTINCT question_bank_question_id FROM edu.question_bank_allocations
                WHERE assessment_id = :assessmentId AND status = 'RESERVED'
                """, Map.of("assessmentId", assessment.id()), (rs, rowNum) -> rs.getObject(1, UUID.class));
        jdbc.update("""
                UPDATE edu.question_bank_allocations SET status = 'USED', used_at = :publishedAt
                WHERE assessment_id = :assessmentId AND status = 'RESERVED'
                """, Map.of("publishedAt", publishedAt, "assessmentId", assessment.id()));
        if (!bankIds.isEmpty()) {
            jdbc.update("""
                    UPDATE edu.question_bank_questions SET recently_used = true, last_used_at = :publishedAt,
                        updated_at = CURRENT_TIMESTAMP WHERE id IN (:ids)
                    """, new MapSqlParameterSource().addValue("publishedAt", publishedAt).addValue("ids", bankIds));
        }
        int updated = jdbc.update("""
                UPDATE edu.assessments SET lifecycle_status = 'PUBLISHED', current_step = 10,
                    published_by = :actorId, published_at = :publishedAt,
                    version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = :assessmentId AND lifecycle_status = 'READY_TO_PUBLISH'
                """, Map.of("actorId", actorId, "publishedAt", publishedAt, "assessmentId", assessment.id()));
        if (updated != 1) throw new IllegalStateException("Assessment changed before publish");
        return new PublishResult(assessment.id(), LifecycleStatus.PUBLISHED, publishedVariants, bankIds.size(), publishedAt);
    }

    public void updateVariantQuestion(UUID assessmentId, UUID variantId, UUID questionId,
            String content, String type, UUID chapterId, UUID cognitiveLevelId,
            List<UUID> learningOutcomeIds, List<AnswerInput> answers) {
        UUID quizId = variantQuiz(assessmentId, variantId);
        int updated = jdbc.update("""
                UPDATE edu.questions SET content = :content, type = :type, chapter_id = :chapterId,
                    cognitive_level_id = :cognitiveLevelId, version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = :questionId AND quiz_id = :quizId
                """, new MapSqlParameterSource().addValue("content", content).addValue("type", type)
                .addValue("chapterId", chapterId).addValue("cognitiveLevelId", cognitiveLevelId)
                .addValue("questionId", questionId).addValue("quizId", quizId));
        if (updated != 1) throw new IllegalArgumentException("Variant question was not found");
        jdbc.update("DELETE FROM edu.question_learning_outcomes WHERE question_id = :id", Map.of("id", questionId));
        for (UUID outcomeId : learningOutcomeIds) {
            jdbc.update("""
                    INSERT INTO edu.question_learning_outcomes (question_id, learning_outcome_id)
                    VALUES (:questionId, :outcomeId)
                    """, Map.of("questionId", questionId, "outcomeId", outcomeId));
        }
        jdbc.update("DELETE FROM edu.answers WHERE question_id = :id", Map.of("id", questionId));
        for (AnswerInput answer : answers) {
            jdbc.update("""
                    INSERT INTO edu.answers (question_id, content, is_correct, explanation, created_at, updated_at)
                    VALUES (:questionId, :content, :correct, :explanation, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, new MapSqlParameterSource().addValue("questionId", questionId)
                    .addValue("content", answer.content()).addValue("correct", answer.correct())
                    .addValue("explanation", answer.explanation()));
        }
    }

    private List<UUID> candidateQuestionIds(AssessmentView assessment, BankPlanCell cell,
            Set<UUID> excluded, int limit) {
        String exclusion = excluded.isEmpty() ? "" : " AND q.id NOT IN (:excluded)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("courseId", assessment.courseId()).addValue("chapterId", cell.chapterId())
                .addValue("levelId", cell.customLearningLevelId()).addValue("levelCode", cell.levelCode())
                .addValue("outcomeIds", cell.learningOutcomeIds().isEmpty() ? List.of(UUID.randomUUID()) : cell.learningOutcomeIds())
                .addValue("outcomeCount", cell.learningOutcomeIds().size()).addValue("allowRecent", assessment.allowRecentlyUsedQuestions())
                .addValue("excluded", excluded).addValue("limit", limit);
        return jdbc.query("""
                SELECT q.id FROM edu.question_bank_questions q
                JOIN edu.cognitive_levels cl ON cl.id = q.cognitive_level_id
                WHERE q.course_id = :courseId AND q.question_type = 'MULTIPLE_CHOICE'
                  AND (:chapterId IS NULL OR q.chapter_id = :chapterId)
                  AND (q.custom_learning_level_id = :levelId OR lower(cl.code) = lower(:levelCode)
                       OR cl.sort_order = (SELECT sort_order FROM edu.custom_learning_levels WHERE id = :levelId))
                  AND (:outcomeCount = 0 OR (SELECT COUNT(DISTINCT qlo.learning_outcome_id)
                       FROM edu.question_bank_question_learning_outcomes qlo
                       WHERE qlo.question_bank_question_id = q.id AND qlo.learning_outcome_id IN (:outcomeIds)) = :outcomeCount)
                  AND (:allowRecent = true OR q.recently_used = false)
                  AND NOT EXISTS (SELECT 1 FROM edu.question_bank_allocations a
                                  WHERE a.question_bank_question_id = q.id AND a.status = 'RESERVED')
                """ + exclusion + " ORDER BY q.recently_used, q.last_used_at NULLS FIRST, q.validated_at, q.id LIMIT :limit",
                params, (rs, rowNum) -> rs.getObject("id", UUID.class));
    }

    private void copyBankQuestionToQuiz(UUID bankId, UUID quizId, int orderIndex, long seed) {
        Map<String, Object> bank = jdbc.queryForMap("""
                SELECT content, question_type, chapter_id, cognitive_level_id
                FROM edu.question_bank_questions WHERE id = :id
                """, Map.of("id", bankId));
        UUID questionId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO edu.questions
                    (id, content, type, quiz_id, review_status, source_type, source_bank_question_id,
                     chapter_id, cognitive_level_id, order_index)
                VALUES (:id, :content, :type, :quizId, 'APPROVED', 'BANK_COPY', :bankId,
                        :chapterId, :cognitiveLevelId, :orderIndex)
                """, new MapSqlParameterSource(bank).addValue("id", questionId).addValue("quizId", quizId)
                .addValue("bankId", bankId).addValue("chapterId", bank.get("chapter_id"))
                .addValue("cognitiveLevelId", bank.get("cognitive_level_id")).addValue("orderIndex", orderIndex));
        List<Map<String, Object>> answers = new ArrayList<>(jdbc.queryForList("""
                SELECT content, is_correct, explanation FROM edu.question_bank_answers
                WHERE question_bank_question_id = :id ORDER BY order_index
                """, Map.of("id", bankId)));
        Collections.shuffle(answers, new Random(seed ^ bankId.getLeastSignificantBits()));
        for (Map<String, Object> answer : answers) {
            jdbc.update("""
                    INSERT INTO edu.answers (question_id, content, is_correct, explanation, created_at, updated_at)
                    VALUES (:questionId, :content, :is_correct, :explanation, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, new MapSqlParameterSource(answer).addValue("questionId", questionId));
        }
        jdbc.update("""
                INSERT INTO edu.question_learning_outcomes (question_id, learning_outcome_id)
                SELECT :questionId, learning_outcome_id FROM edu.question_bank_question_learning_outcomes
                WHERE question_bank_question_id = :bankId
                """, Map.of("questionId", questionId, "bankId", bankId));
    }

    private List<VariantQuestionView> variantQuestions(UUID quizId) {
        return jdbc.query("""
                SELECT id, source_bank_question_id, chapter_id, cognitive_level_id, content, type, order_index
                FROM edu.questions WHERE quiz_id = :quizId ORDER BY order_index, id
                """, Map.of("quizId", quizId), (rs, rowNum) -> {
            UUID questionId = rs.getObject("id", UUID.class);
            return new VariantQuestionView(questionId, rs.getObject("source_bank_question_id", UUID.class),
                    rs.getObject("chapter_id", UUID.class), rs.getObject("cognitive_level_id", UUID.class),
                    rs.getString("content"), rs.getString("type"), questionOutcomeIds(questionId),
                    quizAnswers(questionId), rs.getInt("order_index"));
        });
    }

    private UUID variantQuiz(UUID assessmentId, UUID variantId) {
        return jdbc.query("SELECT quiz_id FROM edu.assessment_variants WHERE id = :variantId AND assessment_id = :assessmentId",
                Map.of("variantId", variantId, "assessmentId", assessmentId),
                (rs, rowNum) -> rs.getObject("quiz_id", UUID.class)).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Assessment variant was not found"));
    }

    private void validateVariantStructure(UUID quizId, int optionCount) {
        Integer invalidMcq = jdbc.queryForObject("""
                SELECT COUNT(*) FROM edu.questions q
                WHERE q.quiz_id = :quizId AND q.type = 'MULTIPLE_CHOICE'
                  AND ((SELECT COUNT(*) FROM edu.answers a WHERE a.question_id = q.id) <> :optionCount
                       OR (SELECT COUNT(*) FROM edu.answers a WHERE a.question_id = q.id AND a.is_correct) <> 1)
                """, Map.of("quizId", quizId, "optionCount", optionCount), Integer.class);
        Integer blank = jdbc.queryForObject("""
                SELECT COUNT(*) FROM edu.questions q WHERE q.quiz_id = :quizId AND btrim(COALESCE(q.content, '')) = ''
                """, Map.of("quizId", quizId), Integer.class);
        if ((invalidMcq != null && invalidMcq > 0) || (blank != null && blank > 0)) {
            throw new IllegalStateException("Variant contains structurally invalid questions");
        }
    }

    private List<BatchCellView> batchCells(UUID batchId) {
        return jdbc.query("""
                SELECT i.id, i.matrix_row_id, i.custom_learning_level_id, r.chapter_id,
                       ch.title AS chapter, r.topic, l.code AS level_code, l.name AS level_name,
                       i.required_count, i.generated_count, i.status, i.error_message, i.retry_count
                FROM edu.assessment_generation_batch_items i
                JOIN edu.exam_matrix_rows r ON r.id = i.matrix_row_id
                JOIN edu.custom_learning_levels l ON l.id = i.custom_learning_level_id
                LEFT JOIN edu.chapters ch ON ch.chapter_id = r.chapter_id
                WHERE i.batch_id = :batchId
                ORDER BY r.order_index, l.sort_order, i.id
                """, Map.of("batchId", batchId), (rs, rowNum) -> {
            UUID rowId = rs.getObject("matrix_row_id", UUID.class);
            UUID levelId = rs.getObject("custom_learning_level_id", UUID.class);
            return new BatchCellView(rs.getObject("id", UUID.class), rowId, levelId,
                    rs.getObject("chapter_id", UUID.class), rs.getString("chapter"), rs.getString("topic"),
                    rs.getString("level_code"), rs.getString("level_name"), cellOutcomeIds(rowId, levelId),
                    rs.getInt("required_count"), rs.getInt("generated_count"), rs.getString("status"),
                    rs.getString("error_message"), rs.getInt("retry_count"));
        });
    }

    private void validateGeneratedDraft(GeneratedDraft draft) {
        if (draft.content() == null || draft.content().isBlank()) throw new IllegalStateException("Generated Draft content is required");
        if (!"MULTIPLE_CHOICE".equals(draft.type())) throw new IllegalStateException("Quiz2 AI may only generate MULTIPLE_CHOICE");
        if (draft.answers() == null || !List.of(4, 5).contains(draft.answers().size())) {
            throw new IllegalStateException("Generated Draft must contain four or five answers");
        }
        if (draft.answers().stream().filter(answer -> answer.correct()).count() != 1) {
            throw new IllegalStateException("Generated Draft must contain exactly one correct answer");
        }
        if (draft.answers().stream().anyMatch(answer -> answer.content() == null || answer.content().isBlank()
                || answer.explanation() == null || answer.explanation().isBlank())) {
            throw new IllegalStateException("Every generated answer requires content and explanation");
        }
    }

    private List<DraftReviewItem> draftItems(UUID assessmentId, String status) {
        return jdbc.query("""
                SELECT d.id, d.generation_batch_id, d.matrix_row_id, d.custom_learning_level_id,
                       d.content, d.question_type, cl.code AS cognitive_level_code, d.chapter_id,
                       d.review_status, d.version, d.rejected_by, d.rejected_at,
                       EXISTS (SELECT 1 FROM edu.question_bank_questions q
                               WHERE q.course_id = d.course_id AND lower(btrim(q.content)) = lower(btrim(d.content))) AS duplicate_warning
                FROM edu.question_drafts d LEFT JOIN edu.cognitive_levels cl ON cl.id = d.cognitive_level_id
                WHERE d.assessment_id = :assessmentId AND d.review_status = :status
                ORDER BY d.updated_at, d.id
                """, Map.of("assessmentId", assessmentId, "status", status), (rs, rowNum) -> {
            UUID id = rs.getObject("id", UUID.class);
            return new DraftReviewItem(id, null, rs.getObject("generation_batch_id", UUID.class),
                    rs.getObject("matrix_row_id", UUID.class), rs.getObject("custom_learning_level_id", UUID.class),
                    rs.getString("content"), rs.getString("question_type"), rs.getString("cognitive_level_code"),
                    rs.getObject("chapter_id", UUID.class), draftOutcomeIds(id), draftAnswers(id),
                    rs.getString("review_status"), rs.getBoolean("duplicate_warning"), false, rs.getLong("version"),
                    rs.getObject("rejected_by", UUID.class), rs.getObject("rejected_at", LocalDateTime.class));
        });
    }

    private List<DraftReviewItem> bankItems(UUID assessmentId) {
        return jdbc.query("""
                SELECT q.id, q.origin_draft_id, q.generation_batch_id, q.matrix_row_id,
                       q.custom_learning_level_id, q.content, q.question_type,
                       cl.code AS cognitive_level_code, q.chapter_id, q.version,
                       q.validated_by, q.validated_at, q.recently_used
                FROM edu.question_bank_questions q LEFT JOIN edu.cognitive_levels cl ON cl.id = q.cognitive_level_id
                WHERE q.assessment_id = :assessmentId ORDER BY q.validated_at, q.id
                """, Map.of("assessmentId", assessmentId), (rs, rowNum) -> {
            UUID id = rs.getObject("id", UUID.class);
            return new DraftReviewItem(rs.getObject("origin_draft_id", UUID.class), id,
                    rs.getObject("generation_batch_id", UUID.class), rs.getObject("matrix_row_id", UUID.class),
                    rs.getObject("custom_learning_level_id", UUID.class), rs.getString("content"),
                    rs.getString("question_type"), rs.getString("cognitive_level_code"),
                    rs.getObject("chapter_id", UUID.class), bankOutcomeIds(id), bankAnswers(id), "VALIDATED", false,
                    rs.getBoolean("recently_used"), rs.getLong("version"),
                    rs.getObject("validated_by", UUID.class), rs.getObject("validated_at", LocalDateTime.class));
        });
    }

    private List<UUID> draftOutcomeIds(UUID draftId) {
        return jdbc.query("SELECT learning_outcome_id FROM edu.question_draft_learning_outcomes WHERE question_draft_id = :id",
                Map.of("id", draftId), (rs, rowNum) -> rs.getObject(1, UUID.class));
    }

    private List<UUID> bankOutcomeIds(UUID bankId) {
        return jdbc.query("SELECT learning_outcome_id FROM edu.question_bank_question_learning_outcomes WHERE question_bank_question_id = :id",
                Map.of("id", bankId), (rs, rowNum) -> rs.getObject(1, UUID.class));
    }

    private List<UUID> questionOutcomeIds(UUID questionId) {
        return jdbc.query("SELECT learning_outcome_id FROM edu.question_learning_outcomes WHERE question_id = :id",
                Map.of("id", questionId), (rs, rowNum) -> rs.getObject(1, UUID.class));
    }

    private List<AnswerView> draftAnswers(UUID draftId) {
        return jdbc.query("""
                SELECT id, content, is_correct, explanation, order_index FROM edu.question_draft_answers
                WHERE question_draft_id = :id ORDER BY order_index
                """, Map.of("id", draftId), this::mapUuidAnswer);
    }

    private List<AnswerView> bankAnswers(UUID bankId) {
        return jdbc.query("""
                SELECT id, content, is_correct, explanation, order_index FROM edu.question_bank_answers
                WHERE question_bank_question_id = :id ORDER BY order_index
                """, Map.of("id", bankId), this::mapUuidAnswer);
    }

    private List<AnswerView> quizAnswers(UUID questionId) {
        return jdbc.query("""
                SELECT answer_number, content, is_correct, explanation FROM edu.answers
                WHERE question_id = :id ORDER BY answer_number
                """, Map.of("id", questionId), (rs, rowNum) -> new AnswerView(null, rs.getString("content"),
                rs.getBoolean("is_correct"), rs.getString("explanation"), rowNum));
    }

    private AnswerView mapUuidAnswer(ResultSet rs, int rowNum) throws SQLException {
        return new AnswerView(rs.getObject("id", UUID.class), rs.getString("content"), rs.getBoolean("is_correct"),
                rs.getString("explanation"), rs.getInt("order_index"));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize coverage snapshot", ex);
        }
    }

    public record DraftProvenance(UUID batchId, UUID matrixRowId, UUID customLearningLevelId) {
    }

    public record AnswerInput(String content, boolean correct, String explanation) {
    }
}
