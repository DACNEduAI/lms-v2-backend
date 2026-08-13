package edu.ai.lms.core.quiz.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.quiz.app.domain.assessment.QuestionAiModels.*;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class QuestionAiRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public record JobRecord(UUID id, UUID courseId, UUID assessmentId, UUID variantId, UUID questionId,
            UUID requestedBy, Mode mode, Status status, Phase phase, String prompt, JsonNode requestPayload,
            JsonNode actionPlan, long baseQuestionVersion, String clarificationAnswer, int attemptCount) {}

    public record DraftRecord(UUID questionId, long baseQuestionVersion, String content,
            RichContentFormat contentFormat, int appliedChangeIndex) {}

    public record PendingArtifact(UUID id, ArtifactType type, String sourceCode, String latexMode,
            String visualKind, String generator, JsonNode spec, JsonNode layoutOverride, String prompt,
            JsonNode critic, JsonNode fallbackMetadata) {}

    @Transactional
    public DraftRecord ensureDraft(UUID assessmentId, UUID variantId, UUID questionId, long expectedVersion) {
        jdbc.update("""
                INSERT INTO edu.question_ai_working_drafts
                    (question_id, assessment_id, variant_id, base_question_version, content, content_format)
                SELECT q.id, :assessmentId, :variantId, q.version, COALESCE(q.content, ''), q.content_format
                FROM edu.questions q
                JOIN edu.assessment_variants av ON av.quiz_id = q.quiz_id
                WHERE q.id = :questionId AND av.id = :variantId AND av.assessment_id = :assessmentId
                  AND q.version = :expectedVersion
                ON CONFLICT (question_id) DO NOTHING
                """, Map.of("assessmentId", assessmentId, "variantId", variantId,
                "questionId", questionId, "expectedVersion", expectedVersion));
        DraftRecord draft = draft(questionId).orElseThrow(() ->
                new IllegalStateException("Question version changed before the AI draft could be created"));
        if (draft.baseQuestionVersion() != expectedVersion) {
            throw new IllegalStateException("AI working draft is based on another question version; save or discard it first");
        }
        return draft;
    }

    public Optional<DraftRecord> draft(UUID questionId) {
        return jdbc.query("""
                SELECT question_id, base_question_version, content, content_format, applied_change_index
                FROM edu.question_ai_working_drafts WHERE question_id = :questionId
                """, Map.of("questionId", questionId), (rs, row) -> new DraftRecord(
                rs.getObject("question_id", UUID.class), rs.getLong("base_question_version"),
                rs.getString("content"), RichContentFormat.fromNullable(rs.getString("content_format")),
                rs.getInt("applied_change_index"))).stream().findFirst();
    }

    public void syncDraftBaseline(UUID questionId, String content, RichContentFormat format) {
        int updated = jdbc.update("""
                UPDATE edu.question_ai_working_drafts SET content = :content, content_format = :format,
                    updated_at = CURRENT_TIMESTAMP WHERE question_id = :questionId AND applied_change_index = 0
                    AND NOT EXISTS (SELECT 1 FROM edu.question_ai_change_sets WHERE question_id = :questionId)
                    AND NOT EXISTS (SELECT 1 FROM edu.question_ai_jobs WHERE question_id = :questionId
                        AND status IN ('QUEUED', 'RUNNING', 'NEEDS_CLARIFICATION'))
                """, Map.of("questionId", questionId, "content", content, "format", format.name()));
        if (updated != 1) throw new IllegalStateException("AI draft already has history; reload it before starting another AI action");
    }

    public UUID createJob(UUID assessmentId, UUID variantId, UUID questionId, UUID requestedBy,
            Mode mode, String prompt, JsonNode payload, long baseVersion) {
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("""
                    INSERT INTO edu.question_ai_jobs
                        (id, assessment_id, variant_id, question_id, requested_by, mode, status, phase,
                         prompt, request_payload, base_question_version)
                    VALUES (:id, :assessmentId, :variantId, :questionId, :requestedBy, :mode,
                            'QUEUED', 'QUEUED', :prompt, CAST(:payload AS jsonb), :baseVersion)
                    """, new MapSqlParameterSource()
                    .addValue("id", id).addValue("assessmentId", assessmentId).addValue("variantId", variantId)
                    .addValue("questionId", questionId).addValue("requestedBy", requestedBy)
                    .addValue("mode", mode.name()).addValue("prompt", prompt).addValue("payload", json(payload))
                    .addValue("baseVersion", baseVersion));
        } catch (DuplicateKeyException exception) {
            throw new IllegalStateException("This question already has an active AI job", exception);
        }
        return id;
    }

    public Optional<JobRecord> jobRecord(UUID jobId) {
        return jdbc.query("""
                SELECT j.*, a.course_id FROM edu.question_ai_jobs j
                JOIN edu.assessments a ON a.id = j.assessment_id WHERE j.id = :jobId
                """, Map.of("jobId", jobId), (rs, row) -> new JobRecord(
                rs.getObject("id", UUID.class), rs.getObject("course_id", UUID.class),
                rs.getObject("assessment_id", UUID.class), rs.getObject("variant_id", UUID.class),
                rs.getObject("question_id", UUID.class), rs.getObject("requested_by", UUID.class),
                Mode.valueOf(rs.getString("mode")), Status.valueOf(rs.getString("status")),
                Phase.valueOf(rs.getString("phase")), rs.getString("prompt"), read(rs.getString("request_payload")),
                read(rs.getString("action_plan")), rs.getLong("base_question_version"),
                rs.getString("clarification_answer"), rs.getInt("attempt_count"))).stream().findFirst();
    }

    public boolean claim(UUID jobId) {
        return jdbc.update("""
                UPDATE edu.question_ai_jobs SET status = 'RUNNING', phase = CASE WHEN mode = 'ORCHESTRATE'
                    THEN 'ORCHESTRATOR' ELSE mode END, attempt_count = attempt_count + 1,
                    lease_until = CURRENT_TIMESTAMP + INTERVAL '4 minutes',
                    started_at = COALESCE(started_at, CURRENT_TIMESTAMP), updated_at = CURRENT_TIMESTAMP
                WHERE id = :jobId AND status = 'QUEUED' AND attempt_count < 2
                """, Map.of("jobId", jobId)) == 1;
    }

    public boolean heartbeat(UUID jobId, int attempt) {
        return jdbc.update("""
                UPDATE edu.question_ai_jobs SET lease_until = CURRENT_TIMESTAMP + INTERVAL '4 minutes',
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :id AND status = 'RUNNING' AND attempt_count = :attempt
                """, Map.of("id", jobId, "attempt", attempt)) == 1;
    }

    public void phase(UUID jobId, Phase phase) {
        jdbc.update("UPDATE edu.question_ai_jobs SET phase = :phase, updated_at = CURRENT_TIMESTAMP WHERE id = :id AND status = 'RUNNING'",
                Map.of("id", jobId, "phase", phase.name()));
    }

    public void plan(UUID jobId, JsonNode plan, int actionCount) {
        jdbc.update("""
                UPDATE edu.question_ai_jobs SET action_plan = CAST(:plan AS jsonb), action_count = :count,
                    updated_at = CURRENT_TIMESTAMP WHERE id = :id
                """, Map.of("id", jobId, "plan", json(plan), "count", actionCount));
    }

    public void needsClarification(UUID jobId, JsonNode plan, String question) {
        jdbc.update("""
                UPDATE edu.question_ai_jobs SET status = 'NEEDS_CLARIFICATION', phase = 'ORCHESTRATOR',
                    action_plan = CAST(:plan AS jsonb), clarification_question = :question,
                    clarification_expires_at = CURRENT_TIMESTAMP + INTERVAL '10 minutes', lease_until = NULL,
                    updated_at = CURRENT_TIMESTAMP WHERE id = :id AND status = 'RUNNING'
                """, Map.of("id", jobId, "plan", json(plan), "question", question));
    }

    public boolean answerClarification(UUID jobId, String answer) {
        return jdbc.update("""
                UPDATE edu.question_ai_jobs SET status = 'QUEUED', phase = 'QUEUED',
                    clarification_answer = :answer, clarification_question = NULL,
                    clarification_expires_at = NULL, attempt_count = 0, updated_at = CURRENT_TIMESTAMP
                WHERE id = :id AND status = 'NEEDS_CLARIFICATION' AND clarification_expires_at > CURRENT_TIMESTAMP
                """, Map.of("id", jobId, "answer", answer)) == 1;
    }

    public UUID startStep(UUID jobId, String actionId, int order, String specialist, int attempt, JsonNode input) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO edu.question_ai_job_steps
                    (id, job_id, action_id, step_order, specialist, status, attempt, input_payload)
                VALUES (:id, :jobId, :actionId, :stepOrder, :specialist, 'RUNNING', :attempt, CAST(:input AS jsonb))
                """, new MapSqlParameterSource().addValue("id", id).addValue("jobId", jobId)
                .addValue("actionId", actionId).addValue("stepOrder", order).addValue("specialist", specialist)
                .addValue("attempt", attempt).addValue("input", json(input)));
        return id;
    }

    public void finishStep(UUID stepId, String status, JsonNode output, String error) {
        jdbc.update("""
                UPDATE edu.question_ai_job_steps SET status = :status, output_payload = CAST(:output AS jsonb),
                    error_message = :error, completed_at = CURRENT_TIMESTAMP WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", stepId).addValue("status", status)
                .addValue("output", json(output)).addValue("error", error));
    }

    @Transactional
    public void apply(UUID jobId, int expectedAttempt, DraftRecord before, String afterContent, RichContentFormat afterFormat,
            List<PendingArtifact> artifacts, String warning) {
        JobRecord job = jobRecord(jobId).orElseThrow();
        if (job.status() != Status.RUNNING || job.attemptCount() != expectedAttempt) {
            throw new IllegalStateException("AI job execution is no longer active");
        }
        Long questionVersion = jdbc.queryForObject("SELECT version FROM edu.questions WHERE id = :id",
                Map.of("id", job.questionId()), Long.class);
        if (questionVersion == null || questionVersion != job.baseQuestionVersion()) {
            throw new IllegalStateException("Question changed while the AI job was running");
        }
        Integer current = jdbc.queryForObject("SELECT applied_change_index FROM edu.question_ai_working_drafts WHERE question_id = :id FOR UPDATE",
                Map.of("id", job.questionId()), Integer.class);
        int next = (current == null ? 0 : current) + 1;
        jdbc.update("DELETE FROM edu.question_ai_change_sets WHERE question_id = :id AND change_index > :current",
                Map.of("id", job.questionId(), "current", current == null ? 0 : current));
        List<Map<String, Object>> revisionChanges = new java.util.ArrayList<>();
        for (PendingArtifact artifact : artifacts) revisionChanges.add(saveArtifact(job, artifact));
        JsonNode artifactRefs = objectMapper.valueToTree(revisionChanges);
        jdbc.update("""
                INSERT INTO edu.question_ai_change_sets
                    (question_id, job_id, change_index, before_content, before_content_format,
                     after_content, after_content_format, artifact_revisions)
                VALUES (:questionId, :jobId, :index, :before, :beforeFormat, :after, :afterFormat,
                        CAST(:artifacts AS jsonb))
                """, Map.of("questionId", job.questionId(), "jobId", jobId, "index", next,
                "before", before.content(), "beforeFormat", before.contentFormat().name(),
                "after", afterContent, "afterFormat", afterFormat.name(), "artifacts", json(artifactRefs)));
        jdbc.update("""
                UPDATE edu.question_ai_working_drafts SET content = :content, content_format = :format,
                    applied_change_index = :index, updated_at = CURRENT_TIMESTAMP WHERE question_id = :questionId
                """, Map.of("questionId", job.questionId(), "content", afterContent, "format", afterFormat.name(), "index", next));
        jdbc.update("""
                UPDATE edu.question_ai_jobs SET status = 'APPLIED_TO_WORKING_DRAFT', phase = 'DONE',
                    warning_message = :warning, lease_until = NULL, completed_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP WHERE id = :jobId AND status = 'RUNNING'
                """, new MapSqlParameterSource().addValue("jobId", jobId).addValue("warning", warning));
    }

    private Map<String, Object> saveArtifact(JobRecord job, PendingArtifact artifact) {
        jdbc.update("""
                INSERT INTO edu.question_ai_artifacts
                    (id, assessment_id, question_id, artifact_type, current_revision, created_by)
                VALUES (:id, :assessmentId, :questionId, :type, 0, :createdBy)
                ON CONFLICT (id) DO NOTHING
                """, Map.of("id", artifact.id(), "assessmentId", job.assessmentId(), "questionId", job.questionId(),
                "type", artifact.type().name(), "createdBy", job.requestedBy()));
        Integer current = jdbc.queryForObject("SELECT current_revision FROM edu.question_ai_artifacts WHERE id = :id AND question_id = :questionId FOR UPDATE",
                Map.of("id", artifact.id(), "questionId", job.questionId()), Integer.class);
        if (current == null) throw new IllegalArgumentException("AI artifact does not belong to this question");
        int revision = current + 1;
        jdbc.update("""
                INSERT INTO edu.question_ai_artifact_revisions
                    (artifact_id, revision, job_id, source_code, source_hash, latex_mode, visual_kind,
                     generator, structured_spec, layout_override, prompt, critic_result, fallback_metadata)
                VALUES (:id, :revision, :jobId, :source, :hash, :latexMode, :visualKind, :generator,
                        CAST(:spec AS jsonb), CAST(:layout AS jsonb), :prompt, CAST(:critic AS jsonb), CAST(:fallback AS jsonb))
                """, new MapSqlParameterSource().addValue("id", artifact.id()).addValue("revision", revision)
                .addValue("jobId", job.id()).addValue("source", artifact.sourceCode()).addValue("hash", sha256(artifact.sourceCode()))
                .addValue("latexMode", artifact.latexMode()).addValue("visualKind", artifact.visualKind())
                .addValue("generator", artifact.generator()).addValue("spec", json(artifact.spec()))
                .addValue("layout", json(artifact.layoutOverride())).addValue("prompt", artifact.prompt())
                .addValue("critic", json(artifact.critic())).addValue("fallback", json(artifact.fallbackMetadata())));
        jdbc.update("UPDATE edu.question_ai_artifacts SET current_revision = :revision, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                Map.of("id", artifact.id(), "revision", revision));
        return Map.of("artifactId", artifact.id().toString(), "type", artifact.type().name(),
                "beforeRevision", current, "afterRevision", revision);
    }

    public void fail(UUID jobId, int expectedAttempt, String code, String message) {
        jdbc.update("""
                UPDATE edu.question_ai_jobs SET status = 'FAILED', phase = 'DONE', error_code = :code,
                    error_message = :message, lease_until = NULL, completed_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP WHERE id = :id AND status = 'RUNNING'
                        AND attempt_count = :attempt
                """, Map.of("id", jobId, "attempt", expectedAttempt, "code", code, "message", message));
    }

    public boolean cancel(UUID jobId) {
        return jdbc.update("""
                UPDATE edu.question_ai_jobs SET status = 'CANCELLED', phase = 'DONE', lease_until = NULL,
                    completed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = :id AND status IN ('QUEUED', 'RUNNING', 'NEEDS_CLARIFICATION')
                """, Map.of("id", jobId)) == 1;
    }

    public void expireJobs() {
        jdbc.update("""
                UPDATE edu.question_ai_jobs SET status = 'QUEUED', phase = 'QUEUED', lease_until = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE status = 'RUNNING' AND lease_until <= CURRENT_TIMESTAMP
                  AND created_at + INTERVAL '10 minutes' > CURRENT_TIMESTAMP AND attempt_count < 2
                """, Map.of());
        jdbc.update("""
                UPDATE edu.question_ai_jobs SET status = 'TIMED_OUT', phase = 'DONE',
                    error_code = 'JOB_TIMEOUT', error_message = 'AI job exceeded its 10 minute deadline',
                    lease_until = NULL, completed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE (status IN ('QUEUED', 'RUNNING') AND created_at + INTERVAL '10 minutes' <= CURRENT_TIMESTAMP)
                   OR (status = 'NEEDS_CLARIFICATION' AND clarification_expires_at <= CURRENT_TIMESTAMP)
                """, Map.of());
    }

    public List<UUID> queuedJobs(int limit) {
        return jdbc.query("SELECT id FROM edu.question_ai_jobs WHERE status = 'QUEUED' ORDER BY created_at LIMIT :limit",
                Map.of("limit", limit), (rs, row) -> rs.getObject("id", UUID.class));
    }

    public List<ArtifactView> artifacts(UUID questionId) {
        return jdbc.query("""
                SELECT a.id, a.artifact_type, a.current_revision, r.* FROM edu.question_ai_artifacts a
                JOIN edu.question_ai_artifact_revisions r ON r.artifact_id = a.id AND r.revision = a.current_revision
                WHERE a.question_id = :questionId ORDER BY a.created_at, a.id
                """, Map.of("questionId", questionId), (rs, row) -> new ArtifactView(
                rs.getObject("id", UUID.class), ArtifactType.valueOf(rs.getString("artifact_type")),
                rs.getInt("current_revision"), rs.getString("source_code"), rs.getString("latex_mode"),
                rs.getString("visual_kind"), rs.getString("generator"), read(rs.getString("structured_spec")),
                read(rs.getString("layout_override")), rs.getString("prompt"), read(rs.getString("critic_result")),
                read(rs.getString("fallback_metadata")), rs.getTimestamp("created_at").toLocalDateTime()));
    }

    public Optional<ArtifactView> artifact(UUID questionId, UUID artifactId) {
        return artifacts(questionId).stream().filter(value -> value.id().equals(artifactId)).findFirst();
    }

    public WorkingDraftView draftView(UUID questionId) {
        DraftRecord draft = draft(questionId).orElseThrow(() -> new IllegalArgumentException("AI working draft was not found"));
        Integer latest = jdbc.queryForObject("SELECT COALESCE(MAX(change_index), 0) FROM edu.question_ai_change_sets WHERE question_id = :id",
                Map.of("id", questionId), Integer.class);
        int max = latest == null ? 0 : latest;
        List<ArtifactView> visibleArtifacts = artifacts(questionId).stream()
                .filter(artifact -> draft.content().contains("data-ai-artifact-id=\"" + artifact.id() + "\"")
                        || draft.content().contains("data-ai-artifact-id='" + artifact.id() + "'"))
                .toList();
        return new WorkingDraftView(questionId, draft.baseQuestionVersion(), draft.content(), draft.contentFormat(),
                draft.appliedChangeIndex(), max, draft.appliedChangeIndex() > 0, draft.appliedChangeIndex() < max,
                visibleArtifacts);
    }

    @Transactional
    public WorkingDraftView moveChangePointer(UUID questionId, boolean redo) {
        DraftRecord draft = draft(questionId).orElseThrow(() -> new IllegalArgumentException("AI working draft was not found"));
        int target = draft.appliedChangeIndex() + (redo ? 1 : -1);
        if (target < 0) throw new IllegalStateException("There is no AI change to undo");
        String content;
        String format;
        int revisionsFrom = redo ? target : draft.appliedChangeIndex();
        JsonNode revisionChanges = jdbc.query("""
                SELECT artifact_revisions FROM edu.question_ai_change_sets
                WHERE question_id = :id AND change_index = :changeIndex
                """, Map.of("id", questionId, "changeIndex", revisionsFrom),
                (rs, row) -> read(rs.getString("artifact_revisions"))).stream().findFirst().orElse(null);
        if (target == 0) {
            Map<String, Object> first = jdbc.queryForMap("""
                    SELECT before_content, before_content_format FROM edu.question_ai_change_sets
                    WHERE question_id = :id AND change_index = 1
                    """, Map.of("id", questionId));
            content = (String) first.get("before_content");
            format = (String) first.get("before_content_format");
        } else {
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT after_content, after_content_format FROM edu.question_ai_change_sets
                    WHERE question_id = :id AND change_index = :target
                    """, Map.of("id", questionId, "target", target));
            if (rows.isEmpty()) throw new IllegalStateException("There is no AI change to redo");
            content = (String) rows.getFirst().get("after_content");
            format = (String) rows.getFirst().get("after_content_format");
        }
        jdbc.update("""
                UPDATE edu.question_ai_working_drafts SET content = :content, content_format = :format,
                    applied_change_index = :target, updated_at = CURRENT_TIMESTAMP WHERE question_id = :id
                """, Map.of("id", questionId, "content", content, "format", format, "target", target));
        if (revisionChanges != null && revisionChanges.isArray()) {
            for (JsonNode change : revisionChanges) {
                int revision = change.path(redo ? "afterRevision" : "beforeRevision").asInt();
                jdbc.update("""
                        UPDATE edu.question_ai_artifacts SET current_revision = :revision, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :artifactId AND question_id = :questionId
                        """, Map.of("artifactId", UUID.fromString(change.path("artifactId").asText()),
                        "questionId", questionId, "revision", revision));
            }
        }
        return draftView(questionId);
    }

    public List<JobView> timeline(UUID questionId) {
        return jdbc.query("""
                SELECT * FROM edu.question_ai_jobs WHERE question_id = :questionId ORDER BY created_at DESC
                """, Map.of("questionId", questionId), (rs, row) -> new JobView(
                rs.getObject("id", UUID.class), rs.getObject("assessment_id", UUID.class),
                rs.getObject("variant_id", UUID.class), rs.getObject("question_id", UUID.class),
                Mode.valueOf(rs.getString("mode")), Status.valueOf(rs.getString("status")),
                Phase.valueOf(rs.getString("phase")), rs.getString("prompt"), read(rs.getString("action_plan")),
                rs.getLong("base_question_version"), rs.getString("clarification_question"),
                rs.getString("error_code"), rs.getString("error_message"), rs.getString("warning_message"),
                rs.getInt("action_count"), rs.getTimestamp("created_at").toLocalDateTime(),
                timestamp(rs.getTimestamp("started_at")), timestamp(rs.getTimestamp("completed_at")),
                steps(rs.getObject("id", UUID.class))));
    }

    public JobView jobView(UUID jobId) {
        JobRecord job = jobRecord(jobId).orElseThrow(() -> new IllegalArgumentException("AI job was not found"));
        return timeline(job.questionId()).stream().filter(value -> value.id().equals(jobId)).findFirst().orElseThrow();
    }

    private List<JobStepView> steps(UUID jobId) {
        return jdbc.query("SELECT * FROM edu.question_ai_job_steps WHERE job_id = :jobId ORDER BY step_order, started_at",
                Map.of("jobId", jobId), (rs, row) -> new JobStepView(rs.getObject("id", UUID.class),
                rs.getString("action_id"), rs.getInt("step_order"), rs.getString("specialist"),
                rs.getString("status"), rs.getInt("attempt"), read(rs.getString("input_payload")),
                read(rs.getString("output_payload")), rs.getString("error_message"),
                rs.getTimestamp("started_at").toLocalDateTime(), timestamp(rs.getTimestamp("completed_at"))));
    }

    public boolean hasBlockingDraftOrJob(UUID assessmentId) {
        Integer count = jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM edu.question_ai_jobs WHERE assessment_id = :id
                        AND status IN ('QUEUED', 'RUNNING', 'NEEDS_CLARIFICATION'))
                     + (SELECT COUNT(*) FROM edu.question_ai_working_drafts WHERE assessment_id = :id)
                """, Map.of("id", assessmentId), Integer.class);
        return count != null && count > 0;
    }

    public List<UUID> blockingQuestionIds(UUID assessmentId) {
        return jdbc.query("""
                SELECT DISTINCT question_id FROM (
                    SELECT question_id FROM edu.question_ai_working_drafts WHERE assessment_id = :id
                    UNION ALL
                    SELECT question_id FROM edu.question_ai_jobs WHERE assessment_id = :id
                        AND status IN ('QUEUED', 'RUNNING', 'NEEDS_CLARIFICATION')
                ) blocked ORDER BY question_id
                """, Map.of("id", assessmentId), (rs, row) -> rs.getObject("question_id", UUID.class));
    }

    public boolean hasActiveJob(UUID questionId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM edu.question_ai_jobs WHERE question_id = :id
                AND status IN ('QUEUED', 'RUNNING', 'NEEDS_CLARIFICATION')
                """, Map.of("id", questionId), Integer.class);
        return count != null && count > 0;
    }

    @Transactional
    public void discardDraft(UUID questionId) {
        // Change sets are an undo/redo journal for the current working-draft session.
        // Job steps and artifact revisions remain append-only as the assessment audit trail.
        jdbc.update("DELETE FROM edu.question_ai_change_sets WHERE question_id = :id", Map.of("id", questionId));
        jdbc.update("DELETE FROM edu.question_ai_working_drafts WHERE question_id = :id", Map.of("id", questionId));
    }

    public void markArtifactStored(UUID artifactId, String storageKey) {
        jdbc.update("""
                UPDATE edu.question_ai_artifact_revisions SET storage_key = :storageKey
                WHERE artifact_id = :artifactId AND revision = (
                    SELECT current_revision FROM edu.question_ai_artifacts WHERE id = :artifactId)
                """, Map.of("artifactId", artifactId, "storageKey", storageKey));
    }

    private String json(JsonNode value) { return value == null ? "null" : value.toString(); }

    private JsonNode read(String value) {
        if (value == null) return null;
        try { return objectMapper.readTree(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Stored AI audit JSON is invalid", exception); }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private LocalDateTime timestamp(java.sql.Timestamp value) { return value == null ? null : value.toLocalDateTime(); }
}
