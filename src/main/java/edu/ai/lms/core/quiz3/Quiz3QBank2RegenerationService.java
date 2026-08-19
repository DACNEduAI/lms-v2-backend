package edu.ai.lms.core.quiz3;

import static edu.ai.lms.core.quiz3.Quiz3Models.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.service.exammatrix.ExamMatrixAccess;
import lombok.RequiredArgsConstructor;

/** A separate full-question working-draft flow for explicit QBank2 examples. */
@Service
@RequiredArgsConstructor
public class Quiz3QBank2RegenerationService {
    private static final Duration AI_TIMEOUT = Duration.ofSeconds(90);
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final WebClient aiWebClient;
    private final ExamMatrixAccess access;
    private final LoadSignedInUserPort actors;
    @Qualifier("questionAiExecutor") private final ThreadPoolTaskExecutor questionAiExecutor;

    @Transactional
    public QBank2RegenerationStartResult start(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId,
            QBank2RegenerateRequest input) {
        CurrentActor actor = requireReviewQuestion(courseId, assessmentId, variantId, questionId);
        List<UUID> exampleIds = normalizeExampleIds(input == null ? null : input.exampleIds());
        List<Example> examples = examplesForCourse(courseId, exampleIds);
        if (examples.size() != exampleIds.size()) throw badRequest("Every QBank2 example must belong to this course");
        long version = jdbc.queryForObject("SELECT version FROM edu.questions WHERE id=?", Long.class, questionId);
        Map<String, Object> payload = requestPayload(courseId, assessmentId, questionId, examples);
        UUID jobId = UUID.randomUUID();
        try {
            jdbc.update("""
                    INSERT INTO edu.quiz3_qbank2_regeneration_jobs
                        (id,assessment_id,variant_id,question_id,requested_by,example_ids,request_payload,base_question_version,status,phase)
                    VALUES (?,?,?,?,?,CAST(? AS jsonb),CAST(? AS jsonb),?,'QUEUED','QUEUED')
                    """, jobId, assessmentId, variantId, questionId, actor.id(), json(exampleIds), json(payload), version);
            return new QBank2RegenerationStartResult(view(jobId, questionId), false);
        } catch (DuplicateKeyException ignored) {
            UUID active = jdbc.query("""
                    SELECT id FROM edu.quiz3_qbank2_regeneration_jobs WHERE question_id=? AND status IN('QUEUED','RUNNING')
                    ORDER BY created_at DESC LIMIT 1
                    """, rs -> rs.next() ? rs.getObject(1, UUID.class) : null, questionId);
            if (active == null) throw ignored;
            return new QBank2RegenerationStartResult(view(active, questionId), true);
        }
    }

    public QBank2RegenerationJobView job(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId, UUID jobId) {
        requireReviewQuestion(courseId, assessmentId, variantId, questionId);
        return view(jobId, questionId);
    }

    @Transactional(readOnly = true)
    public QBank2RegenerationDraft draft(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId) {
        requireReviewQuestion(courseId, assessmentId, variantId, questionId);
        return jdbc.query("""
                SELECT base_question_version,content,content_format,explanation,explanation_format,answers::text,
                       content_group_codes::text,example_ids::text
                FROM edu.quiz3_qbank2_regeneration_drafts WHERE question_id=? AND assessment_id=? AND variant_id=?
                """, rs -> {
            if (!rs.next()) return null;
            return new QBank2RegenerationDraft(questionId, rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), answers(rs.getString(6)), strings(rs.getString(7)), uuids(rs.getString(8)));
        }, questionId, assessmentId, variantId);
    }

    @Transactional
    public void discard(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId) {
        requireReviewQuestion(courseId, assessmentId, variantId, questionId);
        jdbc.update("""
                UPDATE edu.quiz3_qbank2_regeneration_jobs SET status='CANCELLED',phase='DONE',error_code='CANCELLED_BY_USER',
                error_message='QBank2 regeneration was discarded',lease_until=NULL,completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP
                WHERE assessment_id=? AND variant_id=? AND question_id=? AND status IN('QUEUED','RUNNING')
                """, assessmentId, variantId, questionId);
        jdbc.update("DELETE FROM edu.quiz3_qbank2_regeneration_drafts WHERE question_id=? AND assessment_id=? AND variant_id=?",
                questionId, assessmentId, variantId);
    }

    /** Called inside the normal Save question transaction after its optimistic-lock update succeeds. */
    void applyOnQuestionSave(UUID assessmentId, UUID variantId, UUID questionId, long expectedQuestionVersion) {
        Map<String, Object> draft = jdbc.query("""
                SELECT base_question_version,content_group_ids::text,content_group_codes::text,example_ids::text,reference_snapshot::text
                FROM edu.quiz3_qbank2_regeneration_drafts WHERE question_id=? AND assessment_id=? AND variant_id=?
                """, rs -> rs.next() ? Map.of("version", rs.getLong(1), "groupIds", rs.getString(2),
                "groupCodes", rs.getString(3), "exampleIds", rs.getString(4), "referenceSnapshot", rs.getString(5)) : null, questionId, assessmentId, variantId);
        if (draft == null || ((Long) draft.get("version")) != expectedQuestionVersion) return;
        jdbc.update("""
                    UPDATE edu.quiz3_question_provenance SET qbank2_selection_mode='MANUAL_OVERRIDE',
                    qbank2_content_group_ids=CAST(? AS jsonb),qbank2_content_group_codes=CAST(? AS jsonb),
                    qbank2_example_ids=CAST(? AS jsonb),qbank2_reference_snapshot=CAST(? AS jsonb) WHERE question_id=?
                """, draft.get("groupIds"), draft.get("groupCodes"), draft.get("exampleIds"), draft.get("referenceSnapshot"), questionId);
        jdbc.update("DELETE FROM edu.quiz3_qbank2_regeneration_drafts WHERE question_id=?", questionId);
    }

    @Scheduled(fixedDelayString = "${quiz3.qbank2-regeneration.dispatch-delay-ms:1000}")
    public void dispatchQueued() {
        expireStale();
        int available = Math.max(0, questionAiExecutor.getMaxPoolSize() - questionAiExecutor.getActiveCount());
        if (available == 0) return;
        List<UUID> jobs = jdbc.query("SELECT id FROM edu.quiz3_qbank2_regeneration_jobs WHERE status='QUEUED' ORDER BY created_at LIMIT ?",
                (rs, row) -> rs.getObject(1, UUID.class), Math.min(available, 10));
        for (UUID id : jobs) if (claim(id)) questionAiExecutor.execute(() -> process(id));
    }

    private boolean claim(UUID id) {
        return jdbc.update("""
                UPDATE edu.quiz3_qbank2_regeneration_jobs SET status='RUNNING',phase='GENERATING',started_at=CURRENT_TIMESTAMP,
                    lease_until=CURRENT_TIMESTAMP + INTERVAL '3 minutes',updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='QUEUED'
                """, id) == 1;
    }

    private void process(UUID id) {
        try {
            Job job = loadJob(id);
            JsonNode result = aiWebClient.post().uri("/quiz3/questions/regenerate-with-qbank2")
                    .contentType(MediaType.APPLICATION_JSON).bodyValue(aiPayload(job.payload())).retrieve().bodyToMono(JsonNode.class)
                    .block(AI_TIMEOUT);
            if (!running(id)) return;
            if (result == null) throw new IllegalStateException("AI returned no QBank2 draft");
            List<AnswerInput> answers = responseAnswers(result.path("answers"));
            if (answers.size() != 4 || answers.stream().filter(AnswerInput::correct).count() != 1) {
                throw new IllegalStateException("AI must return four answers with exactly one correct answer");
            }
            String content = result.path("content").asText().trim();
            if (content.isEmpty()) throw new IllegalStateException("AI returned an empty question");
            String explanation = result.path("explanation").asText("").trim();
            jdbc.update("""
                    INSERT INTO edu.quiz3_qbank2_regeneration_drafts(question_id,assessment_id,variant_id,job_id,base_question_version,
                        content,content_format,explanation,explanation_format,answers,content_group_ids,content_group_codes,example_ids,reference_snapshot)
                    VALUES (?,?,?,?,? ,?,'PLAIN_TEXT',?,'PLAIN_TEXT',CAST(? AS jsonb),CAST(? AS jsonb),CAST(? AS jsonb),CAST(? AS jsonb),CAST(? AS jsonb))
                    ON CONFLICT(question_id) DO UPDATE SET job_id=EXCLUDED.job_id,base_question_version=EXCLUDED.base_question_version,
                        content=EXCLUDED.content,content_format=EXCLUDED.content_format,explanation=EXCLUDED.explanation,
                        explanation_format=EXCLUDED.explanation_format,answers=EXCLUDED.answers,content_group_ids=EXCLUDED.content_group_ids,
                        content_group_codes=EXCLUDED.content_group_codes,example_ids=EXCLUDED.example_ids,reference_snapshot=EXCLUDED.reference_snapshot,updated_at=CURRENT_TIMESTAMP
                    """, job.questionId(), job.assessmentId(), job.variantId(), job.id(), job.version(), content, explanation, json(answers),
                    json(strings(job.payload().get("contentGroupIds"))), json(strings(job.payload().get("contentGroupCodes"))),
                    json(uuids(job.payload().get("exampleIds"))), json(job.payload().get("referenceSnapshot")));
            jdbc.update("""
                    UPDATE edu.quiz3_qbank2_regeneration_jobs SET status='APPLIED_TO_WORKING_DRAFT',phase='DONE',lease_until=NULL,
                    completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='RUNNING'
                    """, id);
        } catch (Exception ex) {
            jdbc.update("""
                    UPDATE edu.quiz3_qbank2_regeneration_jobs SET status='FAILED',phase='DONE',error_code='QBANK2_GENERATION_FAILED',
                    error_message=?,lease_until=NULL,completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP
                    WHERE id=? AND status='RUNNING'
                    """, message(ex), id);
        }
    }

    private Job loadJob(UUID id) {
        return jdbc.query("SELECT id,assessment_id,variant_id,question_id,base_question_version,request_payload::text FROM edu.quiz3_qbank2_regeneration_jobs WHERE id=? AND status='RUNNING'",
                rs -> {
                    if (!rs.next()) throw new IllegalStateException("QBank2 job is no longer running");
                    return new Job(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getObject(3, UUID.class),
                            rs.getObject(4, UUID.class), rs.getLong(5), object(rs.getString(6)));
                }, id);
    }
    private boolean running(UUID id) { return jdbc.query("SELECT status FROM edu.quiz3_qbank2_regeneration_jobs WHERE id=?", rs -> rs.next() && "RUNNING".equals(rs.getString(1)), id); }

    private void expireStale() {
        jdbc.update("""
                UPDATE edu.quiz3_qbank2_regeneration_jobs SET status='TIMED_OUT',phase='DONE',error_code='JOB_TIMEOUT',
                error_message='QBank2 regeneration job exceeded its time limit',lease_until=NULL,completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP
                WHERE (status='RUNNING' AND (lease_until IS NULL OR lease_until<=CURRENT_TIMESTAMP))
                   OR (status='QUEUED' AND created_at<=CURRENT_TIMESTAMP - INTERVAL '3 minutes')
                """);
    }

    private CurrentActor requireReviewQuestion(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId) {
        access.requireCourseTeacher(courseId);
        CurrentActor actor = actors.findAuthenticatedUser().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required"));
        Integer matches = jdbc.queryForObject("""
                SELECT COUNT(*) FROM edu.quiz3_assessments a JOIN edu.quiz3_assessment_variants v ON v.assessment_id=a.id
                JOIN edu.questions q ON q.quiz_id=v.quiz_id WHERE a.id=? AND a.course_id=? AND a.lifecycle_status='REVIEW_PENDING'
                AND a.current_step=6 AND v.id=? AND v.variant_kind='BASE' AND q.id=?
                """, Integer.class, assessmentId, courseId, variantId, questionId);
        if (matches == null || matches != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "QBank2 regeneration is available only while reviewing Quiz 001");
        return actor;
    }

    static List<UUID> normalizeExampleIds(List<UUID> raw) {
        List<UUID> value = raw == null ? List.of() : raw;
        if (value.size() > 1) throw badRequest("At most one QBank2 example can be selected");
        if (value.stream().anyMatch(java.util.Objects::isNull) || new LinkedHashSet<>(value).size() != value.size()) {
            throw badRequest("QBank2 example IDs must be unique");
        }
        return List.copyOf(value);
    }

    private List<Example> examplesForCourse(UUID courseId, List<UUID> ids) {
        List<Example> value = new ArrayList<>();
        for (UUID id : ids) {
            Example example = jdbc.query("""
                    SELECT e.id,g.id,g.code,g.description,e.content FROM edu.question_bank_2_examples e
                    JOIN edu.question_bank_2_groups g ON g.id=e.group_id WHERE e.id=? AND g.course_id=?
                    """, rs -> rs.next() ? new Example(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
                    rs.getString(3), rs.getString(4), rs.getString(5)) : null, id, courseId);
            if (example != null) value.add(example);
        }
        return value;
    }

    private Map<String, Object> requestPayload(UUID courseId, UUID assessmentId, UUID questionId, List<Example> examples) {
        Map<String, Object> metadata = jdbc.query("""
                SELECT COALESCE(c2.title,c3.title),COALESCE(lo.prefix_number::text || ' - ' || lo.description,lo.description),COALESCE(l2.name,l3.name),
                       COALESCE(p.matrix2_row_id,p.matrix3_row_id),p.learning_outcome_id,COALESCE(p.matrix2_custom_learning_level_id,p.matrix3_custom_learning_level_id)
                FROM edu.quiz3_question_provenance p JOIN edu.learning_outcomes lo ON lo.id=p.learning_outcome_id
                LEFT JOIN edu.exam_matrix_2_rows r2 ON r2.id=p.matrix2_row_id LEFT JOIN edu.chapters c2 ON c2.chapter_id=r2.chapter_id
                LEFT JOIN edu.exam_matrix_3_rows r3 ON r3.id=p.matrix3_row_id LEFT JOIN edu.chapters c3 ON c3.chapter_id=r3.chapter_id
                LEFT JOIN edu.exam_matrix_2_custom_learning_levels l2 ON l2.id=p.matrix2_custom_learning_level_id
                LEFT JOIN edu.exam_matrix_3_custom_learning_levels l3 ON l3.id=p.matrix3_custom_learning_level_id WHERE p.question_id=?
        """, rs -> rs.next() ? Map.of("chapter", rs.getString(1), "learningOutcome", rs.getString(2), "customLearningLevel", rs.getString(3),
                "matrixRowId", String.valueOf(rs.getObject(4)), "learningOutcomeId", String.valueOf(rs.getObject(5)), "customLearningLevelId", String.valueOf(rs.getObject(6))) : null, questionId);
        if (metadata == null) throw badRequest("Question provenance is missing");
        String originalContent = jdbc.query("SELECT content FROM edu.questions WHERE id=?", rs -> rs.next() ? rs.getString(1) : null, questionId);
        List<Map<String, Object>> originalAnswers = jdbc.query("""
                SELECT content,is_correct FROM edu.answers WHERE question_id=? ORDER BY answer_number
                """, (answers, row) -> Map.<String, Object>of("content", answers.getString(1), "isCorrect", answers.getBoolean(2)), questionId);
        if (originalContent == null || originalAnswers.size() != 4) throw badRequest("Original QBank2 question must have four answers");
        Map<String, Object> originalQuestion = Map.of("content", originalContent, "answers", originalAnswers);
        List<Map<String, Object>> fewShot = examples.stream().map(example -> Map.<String, Object>of("groupId", example.groupId().toString(),
                "groupCode", example.groupCode(), "groupDescription", example.groupDescription(), "exampleId", example.id().toString(), "content", example.content())).toList();
        LinkedHashSet<UUID> groupIds = new LinkedHashSet<>(); LinkedHashSet<String> groupCodes = new LinkedHashSet<>();
        for (Example example : examples) { groupIds.add(example.groupId()); groupCodes.add(example.groupCode()); }
        Map<String, Object> value = new LinkedHashMap<>(metadata);
        value.put("courseId", courseId.toString()); value.put("assessmentId", assessmentId.toString()); value.put("fewShotExamples", fewShot);
        value.put("originalQuestion", originalQuestion);
        value.put("contentGroupIds", groupIds.stream().map(UUID::toString).toList()); value.put("contentGroupCodes", List.copyOf(groupCodes));
        value.put("exampleIds", examples.stream().map(example -> example.id().toString()).toList());
        value.put("referenceSnapshot", Map.of("selectionMode", "MANUAL_OVERRIDE", "contentGroup", examples.isEmpty() ? Map.of() : Map.of(
                "id", examples.get(0).groupId().toString(), "code", examples.get(0).groupCode(), "description", examples.get(0).groupDescription()),
                "example", examples.isEmpty() ? Map.of() : Map.of("id", examples.get(0).id().toString(), "content", examples.get(0).content())));
        return value;
    }

    private QBank2RegenerationJobView view(UUID id, UUID questionId) {
        QBank2RegenerationJobView value = jdbc.query("SELECT id,status,phase,error_code,error_message FROM edu.quiz3_qbank2_regeneration_jobs WHERE id=? AND question_id=?",
                rs -> rs.next() ? new QBank2RegenerationJobView(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)) : null, id, questionId);
        if (value == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "QBank2 regeneration job not found");
        return value;
    }

    private List<AnswerInput> responseAnswers(JsonNode node) {
        List<AnswerInput> value = new ArrayList<>();
        for (JsonNode answer : node) value.add(new AnswerInput(answer.path("content").asText(), "PLAIN_TEXT", answer.path("isCorrect").asBoolean()));
        return value;
    }
    private Map<String, Object> aiPayload(Map<String, Object> payload) { Map<String, Object> value = new LinkedHashMap<>(payload); value.remove("contentGroupIds"); value.remove("contentGroupCodes"); value.remove("exampleIds"); value.remove("referenceSnapshot"); return value; }
    private List<AnswerInput> answers(String value) { try { return objectMapper.readValue(value, new TypeReference<List<AnswerInput>>() { }); } catch (Exception ex) { throw new IllegalStateException("Invalid QBank2 draft answers", ex); } }
    private Map<String, Object> object(String value) { try { return objectMapper.readValue(value, new TypeReference<LinkedHashMap<String, Object>>() { }); } catch (Exception ex) { throw new IllegalStateException("Invalid QBank2 job payload", ex); } }
    private List<String> strings(Object value) { if (value instanceof List<?> list) return list.stream().map(String::valueOf).toList(); if (value instanceof String text) return strings(text); return List.of(); }
    private List<String> strings(String value) { try { return objectMapper.readValue(value, new TypeReference<List<String>>() { }); } catch (Exception ex) { throw new IllegalStateException("Invalid QBank2 UUID list", ex); } }
    private List<UUID> uuids(Object value) { return strings(value).stream().map(UUID::fromString).toList(); }
    private List<UUID> uuids(String value) { return strings(value).stream().map(UUID::fromString).toList(); }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception ex) { throw new IllegalStateException("Cannot serialize QBank2 payload", ex); } }
    private static ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static String message(Exception ex) { String value = ex instanceof WebClientResponseException remote ? remote.getResponseBodyAsString() : ex.getMessage(); return value == null || value.isBlank() ? "QBank2 generation failed" : value.substring(0, Math.min(value.length(), 1000)); }
    private record Example(UUID id, UUID groupId, String groupCode, String groupDescription, String content) { }
    private record Job(UUID id, UUID assessmentId, UUID variantId, UUID questionId, long version, Map<String, Object> payload) { }
}
