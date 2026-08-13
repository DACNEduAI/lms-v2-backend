package edu.ai.lms.core.quiz.app.service.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.ai.lms.config.CorrelationIdFilter;
import edu.ai.lms.core.quiz.adapter.out.persistence.QuestionAiRepository;
import edu.ai.lms.core.quiz.adapter.out.persistence.RichContentAssetRepository;
import edu.ai.lms.core.quiz.app.port.out.RichContentAssetStoragePort;
import edu.ai.lms.core.quiz.adapter.out.persistence.QuestionAiRepository.DraftRecord;
import edu.ai.lms.core.quiz.adapter.out.persistence.QuestionAiRepository.JobRecord;
import edu.ai.lms.core.quiz.adapter.out.persistence.QuestionAiRepository.PendingArtifact;
import edu.ai.lms.core.quiz.app.domain.assessment.QuestionAiModels.*;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichTextValue;
import edu.ai.lms.core.quiz.app.service.genquiz.QuestionVisualGenerationService;
import edu.ai.lms.core.quiz.app.service.richcontent.RichContentPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionAiJobProcessor {
    private final QuestionAiRepository repository;
    private final QuestionAiDocumentService documents;
    private final RichContentPolicyService richContentPolicy;
    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper;
    private final QuestionAiSseService events;
    private final RichContentAssetRepository richAssets;
    private final RichContentAssetStoragePort assetStorage;

    @Async("questionAiExecutor")
    public void processClaimedAsync(UUID jobId) { processClaimed(jobId); }

    public void process(UUID jobId) {
        if (!repository.claim(jobId)) return;
        processClaimed(jobId);
    }

    private void processClaimed(UUID jobId) {
        events.push(repository.jobView(jobId));
        JobRecord job = repository.jobRecord(jobId).orElseThrow();
        DraftRecord original = repository.draft(job.questionId()).orElseThrow();
        try {
            Work work = new Work(original.content(), original.contentFormat());
            if (job.mode() == Mode.ORCHESTRATE) runOrchestrator(job, work);
            else runQuickAction(job, work);
            repository.phase(jobId, Phase.APPLYING);
            repository.apply(jobId, job.attemptCount(), original, work.content, work.format, List.copyOf(work.artifacts.values()),
                    work.warnings.isEmpty() ? null : String.join("; ", work.warnings));
        } catch (NeedsClarification ignored) {
            // State was atomically moved to NEEDS_CLARIFICATION by runOrchestrator.
        } catch (StaleExecution ignored) {
            // A cancellation, timeout or restarted attempt owns the durable job now.
        } catch (Exception exception) {
            log.warn("Question AI job {} failed: {}", jobId, exception.getMessage());
            repository.fail(jobId, job.attemptCount(), "AI_JOB_FAILED", publicMessage(exception));
        } finally {
            events.push(repository.jobView(jobId));
        }
    }

    private void runOrchestrator(JobRecord job, Work work) {
        repository.phase(job.id(), Phase.ORCHESTRATOR);
        QuestionAiDocumentService.Projection projection = documents.protectArtifacts(work.content);
        ObjectNode document = objectMapper.createObjectNode();
        document.put("content", projection.content());
        document.put("contentFormat", work.format.name());
        document.set("artifactTokens", objectMapper.valueToTree(projection.tokens()));
        ArrayNode artifacts = document.putArray("artifacts");
        repository.artifacts(job.questionId()).forEach(value -> {
            ObjectNode node = artifacts.addObject();
            node.put("id", value.id().toString()).put("type", value.type().name()).put("sourceCode", value.sourceCode());
            if (value.latexMode() != null) node.put("mode", value.latexMode());
            if (value.visualKind() != null) node.put("visualKind", value.visualKind());
            if (value.generator() != null) node.put("generatorUsed", value.generator());
            if (value.spec() != null) node.set("spec", value.spec());
        });
        ObjectNode request = objectMapper.createObjectNode();
        String prompt = job.prompt();
        if (job.clarificationAnswer() != null) prompt += "\nLàm rõ của giáo viên: " + job.clarificationAnswer();
        request.put("prompt", prompt).set("document", document);
        request.set("anchor", anchor(job.requestPayload()));
        JsonNode plan = call(job, null, 0, "ORCHESTRATOR", "/quiz-gen/questions/actions/plan", request);
        validatePlan(job, plan);
        JsonNode clarification = plan.get("clarificationQuestion");
        if (clarification != null && !clarification.isNull() && !clarification.asText().isBlank()) {
            repository.needsClarification(job.id(), plan, clarification.asText());
            throw new NeedsClarification();
        }
        ArrayNode actions = (ArrayNode) plan.path("actions");
        repository.plan(job.id(), plan, actions.size());
        int order = 1;
        for (JsonNode action : actions) applyAction(job, work, action, order++);
    }

    private void runQuickAction(JobRecord job, Work work) {
        ObjectNode action = objectMapper.createObjectNode();
        String operation = job.requestPayload().path("operation").asText("");
        UUID artifactId = uuid(job.requestPayload().get("artifactId"));
        if (operation.isBlank()) operation = artifactId == null ? "CREATE" : "EDIT";
        String type = switch (job.mode()) {
            case TEXT -> "TEXT_REWRITE";
            case LATEX -> "DELETE".equals(operation) ? "LATEX_DELETE" : "LATEX_" + operation;
            case SVG -> "DELETE".equals(operation) ? "SVG_DELETE" : "SVG_" + operation;
            default -> throw new IllegalArgumentException("Quick action mode is invalid");
        };
        action.put("id", "quick-action").put("type", type).put("target", "QUESTION")
                .put("instruction", job.prompt());
        if (artifactId != null) action.put("artifactId", artifactId.toString());
        action.set("anchor", anchor(job.requestPayload()));
        action.putArray("dependsOn");
        applyAction(job, work, action, 1);
    }

    private void applyAction(JobRecord job, Work work, JsonNode action, int order) {
        String type = action.path("type").asText();
        String instruction = action.path("instruction").asText();
        UUID artifactId = uuid(action.get("artifactId"));
        if ("TEXT_REWRITE".equals(type)) {
            repository.phase(job.id(), Phase.TEXT);
            QuestionAiDocumentService.Projection projection = documents.protectArtifacts(work.content);
            ObjectNode request = objectMapper.createObjectNode().put("prompt", instruction)
                    .put("content", projection.content()).put("contentFormat", work.format.name());
            request.set("protectedArtifactTokens", objectMapper.valueToTree(projection.tokens()));
            JsonNode response = call(job, action.path("id").asText(), order, "TEXT",
                    "/quiz-gen/questions/text/refine", request);
            String restored = documents.restoreArtifacts(response.path("content").asText(), projection);
            RichContentFormat format = RichContentFormat.fromNullable(response.path("contentFormat").asText());
            RichTextValue safe = richContentPolicy.sanitize(new RichTextValue(restored, format),
                    RichContentPolicyService.MAX_PROMPT_HTML_LENGTH);
            work.content = safe.content();
            work.format = safe.contentFormat();
            return;
        }
        if (type.startsWith("LATEX_")) {
            repository.phase(job.id(), Phase.LATEX);
            if (type.endsWith("DELETE")) {
                if (artifactId == null && job.requestPayload().hasNonNull("legacySource")) {
                    work.content = documents.deleteLegacyLatex(work.content,
                            job.requestPayload().path("legacySource").asText(),
                            job.requestPayload().path("legacyLatexMode").asText("INLINE"));
                } else {
                    requireArtifact(job, work, artifactId, ArtifactType.LATEX);
                    work.content = documents.deleteArtifact(work.content, artifactId, "LATEX");
                }
                return;
            }
            boolean edit = type.endsWith("EDIT");
            boolean legacy = edit && artifactId == null && job.requestPayload().hasNonNull("legacySource");
            ArtifactView existing = edit && !legacy ? requireArtifact(job, work, artifactId, ArtifactType.LATEX) : null;
            UUID id = edit && !legacy ? artifactId : UUID.randomUUID();
            if (legacy) {
                String legacySource = job.requestPayload().path("legacySource").asText();
                String legacyMode = job.requestPayload().path("legacyLatexMode").asText("INLINE");
                work.content = documents.tagLegacyLatex(work.content, legacySource, legacyMode, id);
                existing = new ArtifactView(id, ArtifactType.LATEX, 0, legacySource, legacyMode,
                        null, null, null, null, null, null, null, null);
            }
            ObjectNode request = objectMapper.createObjectNode().put("operation", edit ? "EDIT" : "CREATE")
                    .put("prompt", instruction).put("questionContent", work.content).set("anchor", action.path("anchor"));
            if (existing != null) request.put("artifactId", id.toString()).put("existingSource", existing.sourceCode())
                    .put("existingMode", existing.latexMode());
            JsonNode response = call(job, action.path("id").asText(), order, "LATEX",
                    "/quiz-gen/questions/latex/generate", request);
            String source = response.path("sourceCode").asText();
            String mode = response.path("mode").asText();
            work.content = edit ? documents.updateLatex(work.content, id, source, mode)
                    : documents.insertLatex(work.content, id, source, mode, toAnchor(action.path("anchor")));
            RichTextValue safe = richContentPolicy.sanitize(new RichTextValue(work.content, RichContentFormat.TIPTAP_HTML),
                    RichContentPolicyService.MAX_PROMPT_HTML_LENGTH);
            work.content = safe.content();
            work.format = safe.contentFormat();
            work.artifacts.put(id, new PendingArtifact(id, ArtifactType.LATEX, source, mode,
                    null, null, null, null, instruction, null, null));
            return;
        }
        if (type.startsWith("SVG_")) {
            repository.phase(job.id(), Phase.SVG);
            if (type.endsWith("DELETE")) {
                if (artifactId == null && job.requestPayload().hasNonNull("legacySource")) {
                    work.content = documents.deleteLegacySvg(work.content, job.requestPayload().path("legacySource").asText());
                } else {
                    requireArtifact(job, work, artifactId, ArtifactType.SVG);
                    work.content = documents.deleteArtifact(work.content, artifactId, "SVG");
                }
                return;
            }
            boolean edit = type.endsWith("EDIT");
            boolean legacy = edit && artifactId == null && job.requestPayload().hasNonNull("legacySource");
            ArtifactView existing = edit && !legacy ? requireArtifact(job, work, artifactId, ArtifactType.SVG) : null;
            UUID id = edit && !legacy ? artifactId : UUID.randomUUID();
            if (legacy) {
                String legacyUrl = job.requestPayload().path("legacySource").asText();
                var owned = richAssets.findByQuestionId(job.questionId()).stream()
                        .filter(asset -> "image/svg+xml".equals(asset.mediaType())
                                && legacyUrl.equals(assetStorage.publicUrl(asset.storageKey())))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Legacy SVG is not owned by this question"));
                String legacySource = new String(assetStorage.read(owned.storageKey()), StandardCharsets.UTF_8);
                legacySource = QuestionVisualGenerationService.sanitizeInteractiveSvg(legacySource);
                work.content = documents.tagLegacySvg(work.content, legacyUrl, id);
                existing = new ArtifactView(id, ArtifactType.SVG, 0, legacySource, null,
                        "GENERIC", "GENERIC", null, null, null, null, null, null);
            }
            boolean deterministicRender = job.requestPayload().hasNonNull("renderSpec");
            ObjectNode request;
            String endpoint;
            if (deterministicRender) {
                ObjectNode spec = job.requestPayload().path("renderSpec").deepCopy();
                if (job.requestPayload().hasNonNull("layoutOverride")) {
                    spec.set("positions", job.requestPayload().path("layoutOverride"));
                } else if (spec.has("positions")) {
                    spec.remove("positions");
                }
                request = objectMapper.createObjectNode();
                request.set("spec", spec);
                request.put("altText", job.requestPayload().path("altText").asText("Hình minh họa có cấu trúc"));
                endpoint = "/quiz-gen/questions/svg/render";
            } else {
                request = objectMapper.createObjectNode().put("operation", edit ? "EDIT" : "CREATE")
                        .put("prompt", instruction).put("questionContent", work.content).put("artifactId", id.toString());
                endpoint = "/quiz-gen/questions/svg/generate";
            }
            if (existing != null && !deterministicRender) {
                request.put("existingSource", existing.sourceCode());
                if (existing.visualKind() != null) request.put("existingVisualKind", existing.visualKind());
                if (existing.spec() != null) request.set("existingSpec", existing.spec());
            }
            JsonNode response = call(job, action.path("id").asText(), order, "SVG",
                    endpoint, request);
            repository.phase(job.id(), Phase.VALIDATOR);
            UUID validationStep = repository.startStep(job.id(), action.path("id").asText(), order,
                    "VALIDATOR", 1, response);
            String source;
            try {
                source = validateSvg(response);
                repository.finishStep(validationStep, "SUCCEEDED",
                        objectMapper.createObjectNode().put("safe", true), null);
            } catch (RuntimeException exception) {
                repository.finishStep(validationStep, "FAILED", null, publicMessage(exception));
                throw exception;
            }
            if (response.hasNonNull("critic")) {
                repository.phase(job.id(), Phase.CRITIC);
                UUID criticStep = repository.startStep(job.id(), action.path("id").asText(), order,
                        "CRITIC", 1, response.path("critic"));
                repository.finishStep(criticStep, "SUCCEEDED", response.path("critic"), null);
            }
            String alt = response.path("altText").asText("Hình minh họa do AI tạo");
            if (!edit) work.content = documents.insertSvg(work.content, id, alt, artifactUrl(job, id),
                    toAnchor(action.path("anchor")));
            response.path("warnings").forEach(value -> work.warnings.add(value.asText()));
            work.artifacts.put(id, new PendingArtifact(id, ArtifactType.SVG, source, null,
                    response.path("visualKind").asText(), response.path("generatorUsed").asText(),
                    nullable(response.get("spec")), deterministicRender ? nullable(job.requestPayload().get("layoutOverride")) : null,
                    instruction, nullable(response.get("critic")),
                    objectMapper.createObjectNode().put("confidence", response.path("confidence").asDouble())));
            return;
        }
        throw new IllegalArgumentException("Orchestrator returned an unsupported action");
    }

    private JsonNode call(JobRecord job, String actionId, int order, String specialist, String path, JsonNode request) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            requireLease(job);
            UUID step = repository.startStep(job.id(), actionId, order, specialist, attempt, request);
            events.push(repository.jobView(job.id()));
            try {
                JsonNode response = aiWebClient.post().uri(path)
                        .header(CorrelationIdFilter.HEADER_NAME, job.id().toString())
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(request).retrieve()
                        .bodyToMono(JsonNode.class).block(Duration.ofMinutes(3));
                if (response == null) throw new IllegalStateException("AI specialist returned no result");
                requireLease(job);
                repository.finishStep(step, "SUCCEEDED", response, null);
                return response;
            } catch (RuntimeException exception) {
                last = exception;
                repository.finishStep(step, "FAILED", null, publicMessage(exception));
                if (exception instanceof StaleExecution || !repository.heartbeat(job.id(), job.attemptCount())) {
                    throw new StaleExecution();
                }
            }
        }
        throw new IllegalStateException(specialist + " specialist failed after two attempts", last);
    }

    private void validatePlan(JobRecord job, JsonNode plan) {
        JsonNode actionsNode = plan.get("actions");
        if (!(actionsNode instanceof ArrayNode actions) || actions.size() > 12) {
            throw new IllegalArgumentException("Orchestrator action plan is invalid or exceeds 12 actions");
        }
        Set<String> known = new HashSet<>();
        for (JsonNode action : actions) {
            String id = action.path("id").asText();
            String type = action.path("type").asText();
            if (id.isBlank() || !known.add(id) || !"QUESTION".equals(action.path("target").asText())) {
                throw new IllegalArgumentException("Orchestrator returned an invalid action ID or target");
            }
            Set<String> allowed = Set.of("TEXT_REWRITE", "LATEX_CREATE", "LATEX_EDIT", "LATEX_DELETE",
                    "SVG_CREATE", "SVG_EDIT", "SVG_DELETE");
            if (!allowed.contains(type)) throw new IllegalArgumentException("Orchestrator returned an unsupported action type");
            for (JsonNode dependency : action.path("dependsOn")) if (!known.contains(dependency.asText())) {
                throw new IllegalArgumentException("Orchestrator action dependency is cyclic or missing");
            }
            if (type.endsWith("EDIT") || type.endsWith("DELETE")) {
                UUID artifactId = uuid(action.get("artifactId"));
                ArtifactType expected = type.startsWith("SVG") ? ArtifactType.SVG : ArtifactType.LATEX;
                ArtifactView artifact = repository.artifact(job.questionId(), artifactId)
                        .orElseThrow(() -> new IllegalArgumentException("Orchestrator referenced an unknown artifact"));
                if (artifact.type() != expected) throw new IllegalArgumentException("Orchestrator referenced the wrong artifact type");
            }
        }
    }

    private ArtifactView requireArtifact(JobRecord job, Work work, UUID id, ArtifactType type) {
        if (id == null) throw new IllegalArgumentException("Artifact ID is required");
        PendingArtifact staged = work.artifacts.get(id);
        if (staged != null) return new ArtifactView(staged.id(), staged.type(), 0, staged.sourceCode(),
                staged.latexMode(), staged.visualKind(), staged.generator(), staged.spec(), staged.layoutOverride(),
                staged.prompt(), staged.critic(), staged.fallbackMetadata(), null);
        ArtifactView artifact = repository.artifact(job.questionId(), id)
                .orElseThrow(() -> new IllegalArgumentException("AI artifact does not belong to this question"));
        if (artifact.type() != type) throw new IllegalArgumentException("AI artifact type does not match the action");
        return artifact;
    }

    private String validateSvg(JsonNode response) {
        String source = QuestionVisualGenerationService.sanitizeInteractiveSvg(response.path("sourceCode").asText());
        byte[] decoded;
        try { decoded = Base64.getDecoder().decode(response.path("renderedImageBase64").asText()); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Generated SVG payload is not valid base64", exception); }
        String decodedSource = QuestionVisualGenerationService.sanitizeInteractiveSvg(new String(decoded, StandardCharsets.UTF_8));
        if (!source.equals(decodedSource)) throw new IllegalArgumentException("Generated SVG source and payload do not match");
        return source;
    }

    private ObjectNode anchor(JsonNode payload) {
        JsonNode value = payload == null ? null : payload.get("anchor");
        ObjectNode result = objectMapper.createObjectNode();
        result.put("kind", value == null ? "END" : value.path("kind").asText("END"));
        if (value != null && value.hasNonNull("blockId")) result.put("blockId", value.path("blockId").asText());
        if (value != null && value.hasNonNull("offset")) result.put("offset", value.path("offset").asInt());
        return result;
    }

    private Anchor toAnchor(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) return new Anchor("END", null, null);
        return new Anchor(value.path("kind").asText("END"), value.path("blockId").asText(null),
                value.hasNonNull("offset") ? value.path("offset").asInt() : null);
    }

    private UUID uuid(JsonNode value) {
        if (value == null || value.isNull() || value.asText().isBlank()) return null;
        try { return UUID.fromString(value.asText()); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Artifact ID is invalid", exception); }
    }

    private JsonNode nullable(JsonNode value) { return value == null || value.isNull() ? null : value; }

    private String artifactUrl(JobRecord job, UUID artifactId) {
        return "/api/courses/" + job.courseId() + "/assessments/" + job.assessmentId() + "/variants/"
                + job.variantId() + "/questions/" + job.questionId() + "/ai-artifacts/" + artifactId + "/content";
    }

    private String publicMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return "AI processing failed; please try again";
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private void requireLease(JobRecord job) {
        if (!repository.heartbeat(job.id(), job.attemptCount())) throw new StaleExecution();
    }

    private static final class Work {
        private String content;
        private RichContentFormat format;
        private final Map<UUID, PendingArtifact> artifacts = new LinkedHashMap<>();
        private final List<String> warnings = new ArrayList<>();
        private Work(String content, RichContentFormat format) { this.content = content; this.format = format; }
    }

    private static final class NeedsClarification extends RuntimeException {}
    private static final class StaleExecution extends RuntimeException {}
}
