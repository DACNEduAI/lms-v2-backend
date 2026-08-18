package edu.ai.lms.core.quiz3;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.service.exammatrix.ExamMatrixAccess;
import edu.ai.lms.core.quiz.app.domain.assessment.QuestionAiModels.Anchor;
import edu.ai.lms.core.quiz.app.port.out.RichContentAssetStoragePort;
import edu.ai.lms.core.quiz.app.service.assessment.QuestionAiDocumentService;
import lombok.RequiredArgsConstructor;

/** Durable, question-stem-only AI jobs for Quiz3. External AI calls never hold a DB transaction open. */
@Service
@RequiredArgsConstructor
public class Quiz3QuestionAiService {
    private static final Duration ACTION_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration JOB_TIMEOUT = Duration.ofMinutes(3);

    private final JdbcTemplate jdbc;
    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper;
    private final ExamMatrixAccess access;
    private final LoadSignedInUserPort actors;
    private final Quiz3RichContentUploadService uploads;
    private final Quiz3RichContentAssetRepository richAssets;
    private final RichContentAssetStoragePort storage;
    private final QuestionAiDocumentService documents;
    private final Quiz3QuestionAiSseService sse;
    private final TransactionTemplate transactions;
    @Qualifier("questionAiExecutor")
    private final ThreadPoolTaskExecutor questionAiExecutor;

    @Transactional
    public Quiz3Models.AiStartResult start(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId,
            Quiz3Models.AiRequest request) {
        CurrentActor actor = authorize(courseId, assessmentId, variantId, questionId);
        if (request == null || request.prompt() == null || request.prompt().isBlank()) {
            throw new IllegalArgumentException("Question-AI prompt is required");
        }
        String mode = normalizeMode(request.mode());
        Map<String, Object> question = jdbc.queryForMap("SELECT content,content_format,version FROM edu.questions WHERE id=?", questionId);
        long version = ((Number) question.get("version")).longValue();
        if (version != request.expectedQuestionVersion()) throw new IllegalStateException("Question changed; reload before using AI");

        expireStaleJobs();
        UUID jobId = UUID.randomUUID();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("mode", mode);
        payload.put("prompt", request.prompt().trim());
        payload.put("expectedQuestionVersion", request.expectedQuestionVersion());
        payload.put("currentContent", request.currentContent() == null ? String.valueOf(question.get("content")) : request.currentContent());
        payload.put("currentContentFormat", request.currentContentFormat() == null ? String.valueOf(question.get("content_format")) : request.currentContentFormat());
        payload.put("anchor", anchorPayload(request.anchor()));
        try {
            jdbc.update("""
                    INSERT INTO edu.quiz3_question_ai_jobs
                        (id,assessment_id,variant_id,question_id,requested_by,mode,status,phase,prompt,request_payload,
                         base_question_version,attempt_count,action_count)
                    VALUES (?,?,?,?,?,?,'QUEUED','QUEUED',?,CAST(? AS jsonb),?,0,0)
                    """, jobId, assessmentId, variantId, questionId, actor.id(), mode, request.prompt().trim(), json(payload), version);
            return new Quiz3Models.AiStartResult(view(jobId), false);
        } catch (DuplicateKeyException ignored) {
            UUID activeId = jdbc.query("""
                    SELECT id FROM edu.quiz3_question_ai_jobs WHERE question_id=?
                    AND status IN ('QUEUED','RUNNING','NEEDS_CLARIFICATION') ORDER BY created_at DESC LIMIT 1
                    """, rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null, questionId);
            if (activeId == null) throw ignored;
            return new Quiz3Models.AiStartResult(view(activeId), true);
        }
    }

    public Quiz3Models.AiJobView job(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId, UUID jobId) {
        authorize(courseId, assessmentId, variantId, questionId);
        return viewForQuestion(jobId, questionId);
    }

    public SseEmitter stream(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId, UUID jobId) {
        return sse.subscribe(jobId, job(courseId, assessmentId, variantId, questionId, jobId));
    }

    @Transactional
    public Quiz3Models.AiJobView cancel(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId, UUID jobId) {
        authorize(courseId, assessmentId, variantId, questionId);
        jdbc.update("""
                UPDATE edu.quiz3_question_ai_jobs SET status='CANCELLED',phase='DONE',error_code='CANCELLED_BY_USER',
                error_message='Question-AI job was cancelled',lease_until=NULL,completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND question_id=? AND status IN ('QUEUED','RUNNING','NEEDS_CLARIFICATION')
                """, jobId, questionId);
        Quiz3Models.AiJobView value = viewForQuestion(jobId, questionId);
        sse.push(value);
        return value;
    }

    @Scheduled(fixedDelayString = "${quiz3.question-ai.dispatch-delay-ms:1000}")
    public void dispatchQueued() {
        expireStaleJobs();
        int available = Math.max(0, questionAiExecutor.getMaxPoolSize() - questionAiExecutor.getActiveCount());
        if (available == 0) return;
        List<UUID> ids = jdbc.query("SELECT id FROM edu.quiz3_question_ai_jobs WHERE status='QUEUED' ORDER BY created_at LIMIT ?",
                (rs, row) -> UUID.fromString(rs.getString(1)), Math.min(available, 20));
        for (UUID jobId : ids) {
            if (!claim(jobId)) continue;
            try {
                questionAiExecutor.execute(() -> process(jobId));
            } catch (TaskRejectedException ex) {
                fail(jobId, "DISPATCH_REJECTED", "Question-AI worker is unavailable", "FAILED");
            }
        }
    }

    private boolean claim(UUID jobId) {
        boolean claimed = jdbc.update("""
                UPDATE edu.quiz3_question_ai_jobs SET status='RUNNING',phase='QUEUED',attempt_count=attempt_count+1,
                started_at=CURRENT_TIMESTAMP,lease_until=CURRENT_TIMESTAMP + INTERVAL '3 minutes',updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND status='QUEUED'
                """, jobId) == 1;
        if (claimed) notifyJob(jobId);
        return claimed;
    }

    private void process(UUID jobId) {
        List<PendingArtifact> artifacts = new ArrayList<>();
        try {
            Execution job = execution(jobId);
            ensureRunning(jobId);
            ensureDeadline(job.startedAt());
            List<Action> actions = plannedActions(job.request(), job.initial());
            if (actions.isEmpty()) throw new JobFailure("NO_EXECUTABLE_ACTION", "AI did not propose an executable question action");
            jdbc.update("UPDATE edu.quiz3_question_ai_jobs SET action_count=?,action_plan=CAST(? AS jsonb),updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='RUNNING'",
                    actions.size(), json(actions.stream().map(Action::payload).toList()), jobId);

            Working working = job.initial();
            int order = 1;
            for (Action action : actions) {
                ensureRunning(jobId);
                ensureDeadline(job.startedAt());
                jdbc.update("UPDATE edu.quiz3_question_ai_jobs SET phase=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='RUNNING'", action.specialist(), jobId);
                UUID stepId = startStep(jobId, action, order++);
                notifyJob(jobId);
                try {
                    working = execute(action, working, artifacts, job.questionId(), job.assessmentId(), job.actorId(), jobId, stepId);
                    completeStep(stepId, "SUCCEEDED", null, null, null);
                    notifyJob(jobId);
                } catch (Exception ex) {
                    JobFailure failure = failure(ex);
                    completeStep(stepId, "FAILED", failure.code(), failure.getMessage(), failure.diagnostics());
                    throw failure;
                }
            }
            Working finalWorking = working;
            transactions.executeWithoutResult(status -> complete(job, finalWorking, artifacts));
            notifyJob(jobId);
        } catch (JobCancelled ignored) {
            cleanup(artifacts);
            notifyJob(jobId);
        } catch (Exception ex) {
            JobFailure failure = failure(ex);
            cleanup(artifacts);
            fail(jobId, failure.code(), failure.getMessage(), "TIMED_OUT".equals(failure.code()) ? "TIMED_OUT" : "FAILED");
        }
    }

    private void complete(Execution job, Working after, List<PendingArtifact> artifacts) {
        ensureRunning(job.id());
        ensureDeadline(job.startedAt());
        Long currentVersion = jdbc.query("SELECT version FROM edu.questions WHERE id=?", rs -> rs.next() ? rs.getLong(1) : null, job.questionId());
        if (currentVersion == null || currentVersion.longValue() != job.version()) throw new JobFailure("QUESTION_VERSION_CHANGED", "Question changed while Question-AI was running; reload and try again");
        persistArtifacts(artifacts, job.assessmentId(), job.questionId(), job.actorId(), job.id());
        saveWorkingDraft(job.questionId(), job.assessmentId(), job.variantId(), job.version(), job.initial(), after, job.id());
        if (jdbc.update("""
                UPDATE edu.quiz3_question_ai_jobs SET status='APPLIED_TO_WORKING_DRAFT',phase='DONE',lease_until=NULL,
                completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND status='RUNNING' AND lease_until>CURRENT_TIMESTAMP
                """, job.id()) != 1) throw new JobCancelled();
    }

    private void expireStaleJobs() {
        List<UUID> ids = jdbc.query("""
                UPDATE edu.quiz3_question_ai_jobs SET status=CASE WHEN status='NEEDS_CLARIFICATION' THEN 'FAILED' ELSE 'TIMED_OUT' END,
                phase='DONE',error_code=CASE WHEN status='NEEDS_CLARIFICATION' THEN 'PLAN_NEEDS_CLARIFICATION' ELSE 'JOB_TIMEOUT' END,
                error_message=CASE WHEN status='NEEDS_CLARIFICATION' THEN COALESCE(clarification_question,'AI needs clarification; please try again') ELSE 'Question-AI job exceeded its time limit' END,
                lease_until=NULL,completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP
                WHERE status='NEEDS_CLARIFICATION'
                   OR (status='RUNNING' AND (lease_until IS NULL OR lease_until<=CURRENT_TIMESTAMP))
                   OR (status='QUEUED' AND created_at<=CURRENT_TIMESTAMP - INTERVAL '3 minutes')
                RETURNING id
                """, (rs, row) -> UUID.fromString(rs.getString(1)));
        ids.forEach(this::notifyJob);
    }

    private Execution execution(UUID jobId) {
        return jdbc.query("""
                SELECT id,assessment_id,variant_id,question_id,requested_by,base_question_version,request_payload,started_at
                FROM edu.quiz3_question_ai_jobs WHERE id=? AND status='RUNNING'
                """, rs -> {
            if (!rs.next()) throw new JobCancelled();
            try {
                Quiz3Models.AiRequest request = objectMapper.readValue(rs.getString(7), Quiz3Models.AiRequest.class);
                Working initial = new Working(request.currentContent(), "TIPTAP_HTML".equals(request.currentContentFormat()) ? "TIPTAP_HTML" : "PLAIN_TEXT");
                OffsetDateTime started = rs.getObject(8, OffsetDateTime.class);
                return new Execution(UUID.fromString(rs.getString(1)), UUID.fromString(rs.getString(2)), UUID.fromString(rs.getString(3)),
                        UUID.fromString(rs.getString(4)), UUID.fromString(rs.getString(5)), rs.getLong(6), request, initial,
                        started == null ? Instant.now() : started.toInstant());
            } catch (Exception ex) { throw new JobFailure("INVALID_JOB_PAYLOAD", "Question-AI job payload is invalid"); }
        }, jobId);
    }

    private List<Action> plannedActions(Quiz3Models.AiRequest request, Working initial) {
        String mode = normalizeMode(request.mode());
        if (!"ORCHESTRATE".equals(mode)) return List.of(new Action("manual-" + mode.toLowerCase(), mode, request.prompt().trim(), anchor(request.anchor())));
        QuestionAiDocumentService.Projection projection = documents.protectArtifacts(asHtml(initial));
        JsonNode plan = requestAi("/quiz-gen/questions/actions/plan", Map.of(
                "prompt", request.prompt().trim(),
                "document", Map.of("content", projection.content(), "contentFormat", "TIPTAP_HTML", "artifactTokens", projection.tokens(), "artifacts", List.of()),
                "anchor", anchorPayload(request.anchor())));
        String clarification = plan.path("clarificationQuestion").asText("").trim();
        if (!clarification.isBlank()) throw new JobFailure("PLAN_NEEDS_CLARIFICATION", clarification);
        List<Action> actions = new ArrayList<>();
        for (JsonNode value : plan.path("actions")) {
            String specialist = switch (value.path("type").asText()) {
                case "TEXT_REWRITE" -> "TEXT";
                case "LATEX_CREATE" -> "LATEX";
                case "SVG_CREATE" -> "SVG";
                default -> throw new JobFailure("UNSUPPORTED_ACTION", "Quiz3 orchestration only supports text, LaTeX, or SVG creation");
            };
            JsonNode rawAnchor = value.path("anchor");
            Anchor actionAnchor = rawAnchor.has("offset") ? new Anchor(rawAnchor.path("kind").asText("END"), null, rawAnchor.path("offset").asInt()) : anchor(request.anchor());
            actions.add(new Action(value.path("id").asText("action-" + (actions.size() + 1)), specialist,
                    value.path("instruction").asText(request.prompt()), actionAnchor));
        }
        return actions;
    }

    private Working execute(Action action, Working working, List<PendingArtifact> artifacts, UUID questionId,
            UUID assessmentId, UUID actorId, UUID jobId, UUID stepId) {
        return switch (action.specialist()) {
            case "TEXT" -> executeText(action, working);
            case "LATEX" -> executeLatex(action, working, artifacts);
            case "SVG" -> executeSvg(action, working, artifacts, jobId, stepId);
            default -> throw new JobFailure("UNSUPPORTED_ACTION", "Unsupported Quiz3 AI specialist");
        };
    }

    private Working executeText(Action action, Working working) {
        QuestionAiDocumentService.Projection projection = documents.protectArtifacts(asHtml(working));
        JsonNode response = requestAi("/quiz-gen/questions/text/refine", Map.of("prompt", action.instruction(),
                "content", projection.content(), "contentFormat", "TIPTAP_HTML", "protectedArtifactTokens", projection.tokens()));
        String content = response.path("content").asText();
        if (content.isBlank()) throw new JobFailure("EMPTY_TEXT_RESULT", "Question-AI returned no text content");
        return new Working(documents.restoreArtifacts(content, projection), "TIPTAP_HTML");
    }

    private Working executeLatex(Action action, Working working, List<PendingArtifact> artifacts) {
        JsonNode response = requestAi("/quiz-gen/questions/latex/generate", Map.of("operation", "CREATE", "prompt", action.instruction(),
                "questionContent", working.content(), "anchor", anchorPayload(action.anchor())));
        String source = response.path("sourceCode").asText();
        if (source.isBlank()) throw new JobFailure("EMPTY_LATEX_RESULT", "Question-AI returned an empty LaTeX artifact");
        UUID artifactId = UUID.randomUUID();
        String latexMode = response.path("mode").asText("INLINE");
        artifacts.add(PendingArtifact.latex(artifactId, source, latexMode, action.instruction()));
        return new Working(documents.insertLatex(asHtml(working), artifactId, source, latexMode, action.anchor()), "TIPTAP_HTML");
    }

    private Working executeSvg(Action action, Working working, List<PendingArtifact> artifacts, UUID jobId, UUID stepId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operation", "CREATE");
        payload.put("prompt", action.instruction());
        payload.put("questionContent", working.content());
        payload.put("altText", action.instruction());
        payload.put("jobId", jobId);
        JsonNode response = requestAi("/quiz-gen/questions/svg/generate", payload);
        String source = response.path("sourceCode").asText();
        if (source.isBlank()) throw new JobFailure("EMPTY_SVG_RESULT", "Question-AI returned an empty SVG artifact");
        saveSvgStepOutput(stepId, jobId, response);
        Quiz3RichContentUploadService.UploadedAsset uploaded = uploads.uploadAiSvg(source);
        UUID artifactId = UUID.randomUUID();
        artifacts.add(PendingArtifact.svg(artifactId, source, action.instruction(), uploaded,
                response.path("visualKind").asText(null), response.path("generatorUsed").asText(null),
                response.path("spec").isMissingNode() || response.path("spec").isNull() ? null : response.path("spec").toString(),
                response.path("critic").isMissingNode() || response.path("critic").isNull() ? null : response.path("critic").toString()));
        return new Working(documents.insertInlineSvg(asHtml(working), artifactId, response.path("altText").asText(action.instruction()), uploaded.publicUrl(), action.anchor()), "TIPTAP_HTML");
    }

    private void persistArtifacts(List<PendingArtifact> artifacts, UUID assessmentId, UUID questionId, UUID actorId, UUID jobId) {
        for (PendingArtifact artifact : artifacts) {
            jdbc.update("INSERT INTO edu.quiz3_question_ai_artifacts(id,assessment_id,question_id,artifact_type,current_revision,created_by) VALUES (?,?,?,?,1,?)",
                    artifact.id(), assessmentId, questionId, artifact.type(), actorId);
            jdbc.update("""
                    INSERT INTO edu.quiz3_question_ai_artifact_revisions(artifact_id,revision,job_id,source_code,source_hash,latex_mode,visual_kind,generator,structured_spec,prompt,critic_result,storage_key)
                    VALUES (?,1,?,?,?,?,?,?,CAST(? AS jsonb),?,CAST(? AS jsonb),?)
                    """, artifact.id(), jobId, artifact.source(), sha256(artifact.source()), artifact.latexMode(), artifact.visualKind(), artifact.generator(),
                    artifact.spec(), artifact.prompt(), artifact.critic(), artifact.uploaded() == null ? null : artifact.uploaded().storageKey());
            if (artifact.uploaded() != null) richAssets.save(new Quiz3RichContentAsset(UUID.randomUUID(), questionId,
                    artifact.uploaded().storageKey(), artifact.uploaded().mediaType(), artifact.uploaded().byteSize(),
                    artifact.uploaded().sha256(), artifact.id(), null, null));
        }
    }

    private Quiz3Models.AiDraft saveWorkingDraft(UUID questionId, UUID assessmentId, UUID variantId, long version,
            Working before, Working after, UUID jobId) {
        jdbc.update("""
                INSERT INTO edu.quiz3_question_ai_working_drafts(question_id,assessment_id,variant_id,base_question_version,content,content_format)
                VALUES (?,?,?,?,?,?) ON CONFLICT(question_id) DO UPDATE SET content=EXCLUDED.content,content_format=EXCLUDED.content_format,
                base_question_version=EXCLUDED.base_question_version,updated_at=CURRENT_TIMESTAMP
                """, questionId, assessmentId, variantId, version, after.content(), after.format());
        int next = jdbc.queryForObject("SELECT COALESCE(MAX(change_index),0)+1 FROM edu.quiz3_question_ai_change_sets WHERE question_id=?", Integer.class, questionId);
        jdbc.update("DELETE FROM edu.quiz3_question_ai_change_sets WHERE question_id=? AND change_index>=?", questionId, next);
        jdbc.update("""
                INSERT INTO edu.quiz3_question_ai_change_sets(id,question_id,job_id,change_index,before_content,before_content_format,after_content,after_content_format)
                VALUES (?,?,?,?,?,?,?,?)
                """, UUID.randomUUID(), questionId, jobId, next, before.content(), before.format(), after.content(), after.format());
        jdbc.update("UPDATE edu.quiz3_question_ai_working_drafts SET applied_change_index=?,updated_at=CURRENT_TIMESTAMP WHERE question_id=?", next, questionId);
        return new Quiz3Models.AiDraft(questionId, version, after.content(), after.format(), next, next, true, false);
    }

    private UUID startStep(UUID jobId, Action action, int order) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO edu.quiz3_question_ai_job_steps(id,job_id,action_id,step_order,specialist,status,input_payload)
                VALUES (?,?,?,?,?,'RUNNING',CAST(? AS jsonb))
                """, id, jobId, action.id(), order, action.specialist(), json(action.payload()));
        return id;
    }

    private void completeStep(UUID stepId, String status, String code, String message, JsonNode outputPayload) {
        jdbc.update("UPDATE edu.quiz3_question_ai_job_steps SET status=?,error_message=?,output_payload=COALESCE(CAST(? AS jsonb),output_payload),completed_at=CURRENT_TIMESTAMP WHERE id=?", status,
                code == null ? null : code + ": " + message, outputPayload == null ? null : outputPayload.toString(), stepId);
    }

    private void saveSvgStepOutput(UUID stepId, UUID jobId, JsonNode response) {
        ObjectNode output = objectMapper.createObjectNode();
        if (response.has("diagnostics")) output.set("diagnostics", response.path("diagnostics"));
        if (response.has("warnings")) output.set("warnings", response.path("warnings"));
        jdbc.update("UPDATE edu.quiz3_question_ai_job_steps SET output_payload=CAST(? AS jsonb) WHERE id=?",
                output.toString(), stepId);
        if (response.path("warnings").isArray() && !response.path("warnings").isEmpty()) {
            List<String> warnings = new ArrayList<>();
            for (JsonNode warning : response.path("warnings")) warnings.add(warning.asText());
            jdbc.update("UPDATE edu.quiz3_question_ai_jobs SET warning_message=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='RUNNING'",
                    trim(String.join("; ", warnings)), jobId);
        }
    }

    private JsonNode requestAi(String path, Map<String, Object> payload) {
        try {
            return aiWebClient.post().uri(path).contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.readTree(json(payload))).retrieve().bodyToMono(JsonNode.class).block(ACTION_TIMEOUT);
        } catch (WebClientResponseException ex) {
            JsonNode error = upstreamError(ex);
            throw new JobFailure("UPSTREAM_" + ex.getStatusCode().value(), upstreamDetail(ex, error), diagnostics(error));
        } catch (Exception ex) {
            throw new JobFailure("UPSTREAM_REQUEST_FAILED", "Question-AI specialist failed: " + safeMessage(ex));
        }
    }

    private JsonNode upstreamError(WebClientResponseException ex) {
        try { return objectMapper.readTree(ex.getResponseBodyAsString()); }
        catch (Exception ignored) { return objectMapper.createObjectNode(); }
    }

    private String upstreamDetail(WebClientResponseException ex, JsonNode parsed) {
        String body = ex.getResponseBodyAsString();
        JsonNode detail = parsed.path("detail");
        if (!detail.isMissingNode() && !detail.isNull()) {
            body = detail.isTextual() ? detail.asText() : detail.path("message").asText(detail.toString());
        }
        return "AI Pipeline HTTP " + ex.getStatusCode().value() + ": " + trim(body == null || body.isBlank() ? ex.getStatusText() : body);
    }

    private static JsonNode diagnostics(JsonNode error) {
        JsonNode detail = error.path("detail");
        JsonNode diagnostics = detail.path("diagnostics");
        if (diagnostics.isMissingNode() || diagnostics.isNull()) diagnostics = error.path("diagnostics");
        return diagnostics.isMissingNode() || diagnostics.isNull() ? null : diagnostics;
    }

    private void ensureRunning(UUID jobId) {
        String status = jdbc.query("SELECT status FROM edu.quiz3_question_ai_jobs WHERE id=?", rs -> rs.next() ? rs.getString(1) : null, jobId);
        if (!"RUNNING".equals(status)) throw new JobCancelled();
    }

    private void ensureDeadline(Instant startedAt) {
        if (Instant.now().isAfter(startedAt.plus(JOB_TIMEOUT))) throw new JobFailure("TIMED_OUT", "Question-AI job exceeded its three-minute limit");
    }

    private void fail(UUID jobId, String code, String message, String status) {
        jdbc.update("""
                UPDATE edu.quiz3_question_ai_jobs SET status=?,phase='DONE',error_code=?,error_message=?,lease_until=NULL,
                completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND status='RUNNING'
                """, status, code, trim(message), jobId);
        notifyJob(jobId);
    }

    private void cleanup(List<PendingArtifact> artifacts) {
        uploads.cleanup(artifacts.stream().map(PendingArtifact::uploaded).filter(value -> value != null).toList());
    }

    private Quiz3Models.AiJobView viewForQuestion(UUID jobId, UUID questionId) {
        UUID found = jdbc.query("SELECT id FROM edu.quiz3_question_ai_jobs WHERE id=? AND question_id=?", rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null, jobId, questionId);
        if (found == null) throw new IllegalArgumentException("Question-AI job does not exist");
        return view(jobId);
    }

    private Quiz3Models.AiJobView view(UUID jobId) {
        return jdbc.query("SELECT status,phase,error_code,error_message,warning_message FROM edu.quiz3_question_ai_jobs WHERE id=?", rs -> {
            if (!rs.next()) throw new IllegalArgumentException("Question-AI job does not exist");
            List<Quiz3Models.AiStep> steps = jdbc.query("""
                    SELECT action_id,specialist,status,error_message FROM edu.quiz3_question_ai_job_steps
                    WHERE job_id=? ORDER BY step_order
                    """, (step, row) -> {
                String value = step.getString(4);
                String code = value != null && value.contains(": ") ? value.substring(0, value.indexOf(": ")) : null;
                String message = value != null && value.contains(": ") ? value.substring(value.indexOf(": ") + 2) : value;
                return new Quiz3Models.AiStep(step.getString(1), step.getString(2), step.getString(3), code, message);
            }, jobId);
            return new Quiz3Models.AiJobView(jobId, rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), steps);
        }, jobId);
    }

    private void notifyJob(UUID jobId) {
        try { sse.push(view(jobId)); } catch (RuntimeException ignored) { }
    }

    @Transactional
    public Quiz3Models.AiDraft draft(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId) {
        authorize(courseId, assessmentId, variantId, questionId);
        return jdbc.query("""
                SELECT d.base_question_version,d.content,d.content_format,d.applied_change_index,
                       COALESCE((SELECT MAX(c.change_index) FROM edu.quiz3_question_ai_change_sets c WHERE c.question_id=d.question_id),0)
                FROM edu.quiz3_question_ai_working_drafts d WHERE d.question_id=?
                """, rs -> rs.next() ? new Quiz3Models.AiDraft(questionId, rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getInt(5), rs.getInt(4) > 0, rs.getInt(4) < rs.getInt(5)) : null, questionId);
    }

    @Transactional
    public Quiz3Models.AiDraft move(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId, boolean redo) {
        Quiz3Models.AiDraft current = draft(courseId, assessmentId, variantId, questionId);
        if (current == null) throw new IllegalStateException("Question-AI working draft does not exist");
        int target = redo ? current.appliedChangeIndex() + 1 : current.appliedChangeIndex() - 1;
        if (target < 0 || target > current.latestChangeIndex()) throw new IllegalStateException("Question-AI history cannot move further");
        String content; String format;
        if (target == 0) { var value = jdbc.queryForMap("SELECT before_content,before_content_format FROM edu.quiz3_question_ai_change_sets WHERE question_id=? AND change_index=1", questionId); content = String.valueOf(value.get("before_content")); format = String.valueOf(value.get("before_content_format")); }
        else { var value = jdbc.queryForMap("SELECT after_content,after_content_format FROM edu.quiz3_question_ai_change_sets WHERE question_id=? AND change_index=?", questionId, target); content = String.valueOf(value.get("after_content")); format = String.valueOf(value.get("after_content_format")); }
        jdbc.update("UPDATE edu.quiz3_question_ai_working_drafts SET content=?,content_format=?,applied_change_index=?,updated_at=CURRENT_TIMESTAMP WHERE question_id=?", content, format, target, questionId);
        return draft(courseId, assessmentId, variantId, questionId);
    }

    @Transactional
    public void discard(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId) {
        authorize(courseId, assessmentId, variantId, questionId);
        Long baseVersion = jdbc.query("SELECT base_question_version FROM edu.quiz3_question_ai_working_drafts WHERE question_id=?", rs -> rs.next() ? rs.getLong(1) : null, questionId);
        if (baseVersion == null) return;
        List<UUID> artifactIds = jdbc.query("""
                SELECT DISTINCT a.id FROM edu.quiz3_question_ai_artifacts a
                JOIN edu.quiz3_question_ai_artifact_revisions r ON r.artifact_id=a.id
                JOIN edu.quiz3_question_ai_jobs j ON j.id=r.job_id
                WHERE a.question_id=? AND j.base_question_version=?
                """, (rs, row) -> rs.getObject(1, UUID.class), questionId, baseVersion);
        List<String> keys = richAssets.storageKeysForArtifactIds(artifactIds);
        jdbc.update("DELETE FROM edu.quiz3_question_ai_artifacts WHERE id IN (SELECT a.id FROM edu.quiz3_question_ai_artifacts a JOIN edu.quiz3_question_ai_artifact_revisions r ON r.artifact_id=a.id JOIN edu.quiz3_question_ai_jobs j ON j.id=r.job_id WHERE a.question_id=? AND j.base_question_version=?)", questionId, baseVersion);
        jdbc.update("DELETE FROM edu.quiz3_question_ai_change_sets WHERE question_id=? AND job_id IN (SELECT id FROM edu.quiz3_question_ai_jobs WHERE question_id=? AND base_question_version=?)", questionId, questionId, baseVersion);
        jdbc.update("DELETE FROM edu.quiz3_question_ai_working_drafts WHERE question_id=?", questionId);
        keys.forEach(key -> { try { storage.delete(key); } catch (RuntimeException ignored) { } });
    }

    private CurrentActor authorize(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId) {
        access.requireCourseTeacher(courseId);
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM edu.quiz3_assessments a JOIN edu.quiz3_assessment_variants v ON v.assessment_id=a.id
                JOIN edu.questions q ON q.quiz_id=v.quiz_id WHERE a.id=? AND a.course_id=? AND v.id=? AND q.id=?
                """, Integer.class, assessmentId, courseId, variantId, questionId);
        if (count == null || count != 1) throw new IllegalArgumentException("Quiz3 question context is invalid");
        return actors.findAuthenticatedUser().orElseThrow();
    }

    private static String normalizeMode(String value) {
        String mode = value == null ? "TEXT" : value.toUpperCase();
        if (!List.of("ORCHESTRATE", "TEXT", "LATEX", "SVG").contains(mode)) throw new IllegalArgumentException("Quiz3 Question-AI mode must be ORCHESTRATE, TEXT, LATEX, or SVG");
        return mode;
    }

    private static Anchor anchor(Quiz3Models.AiAnchor value) { return value != null && "CURSOR".equals(value.kind()) && value.offset() != null ? new Anchor("CURSOR", null, value.offset()) : new Anchor("END", null, null); }
    private static Map<String, Object> anchorPayload(Quiz3Models.AiAnchor value) { return anchorPayload(anchor(value)); }
    private static Map<String, Object> anchorPayload(Anchor value) { Map<String, Object> payload = new LinkedHashMap<>(); payload.put("kind", value.kind()); if (value.offset() != null) payload.put("offset", value.offset()); return payload; }
    private static String asHtml(Working value) { return "TIPTAP_HTML".equals(value.format()) ? value.content() : "<p>" + org.jsoup.nodes.Entities.escape(value.content()) + "</p>"; }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception ex) { throw new IllegalStateException(ex); } }
    private static String sha256(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception ex) { throw new IllegalStateException(ex); } }
    private static String trim(String value) { if (value == null || value.isBlank()) return "Question-AI request failed"; return value.length() > 1000 ? value.substring(0, 1000) : value; }
    private static String safeMessage(Exception value) { return trim(value.getMessage()); }
    private static JobFailure failure(Exception value) { return value instanceof JobFailure known ? known : value instanceof JobCancelled ? new JobFailure("CANCELLED_BY_USER", "Question-AI job was cancelled") : new JobFailure("UNEXPECTED_ERROR", safeMessage(value)); }

    private record Working(String content, String format) { }
    private record Execution(UUID id, UUID assessmentId, UUID variantId, UUID questionId, UUID actorId, long version, Quiz3Models.AiRequest request, Working initial, Instant startedAt) { }
    private record Action(String id, String specialist, String instruction, Anchor anchor) { Map<String, Object> payload() { Map<String, Object> value = new LinkedHashMap<>(); value.put("id", id); value.put("specialist", specialist); value.put("instruction", instruction); value.put("anchor", anchorPayload(anchor)); return value; } }
    private record PendingArtifact(UUID id, String type, String source, String latexMode, String prompt, Quiz3RichContentUploadService.UploadedAsset uploaded, String visualKind, String generator, String spec, String critic) {
        static PendingArtifact latex(UUID id, String source, String mode, String prompt) { return new PendingArtifact(id, "LATEX", source, mode, prompt, null, null, null, null, null); }
        static PendingArtifact svg(UUID id, String source, String prompt, Quiz3RichContentUploadService.UploadedAsset uploaded, String visualKind, String generator, String spec, String critic) { return new PendingArtifact(id, "SVG", source, null, prompt, uploaded, visualKind, generator, spec, critic); }
    }
    private static final class JobCancelled extends RuntimeException { }
    private static final class JobFailure extends RuntimeException {
        private final String code;
        private final JsonNode diagnostics;
        JobFailure(String code, String message) { this(code, message, null); }
        JobFailure(String code, String message, JsonNode diagnostics) { super(message); this.code = code; this.diagnostics = diagnostics; }
        String code() { return code; }
        JsonNode diagnostics() { return diagnostics; }
    }
}
