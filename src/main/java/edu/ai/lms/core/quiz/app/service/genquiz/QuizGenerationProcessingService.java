package edu.ai.lms.core.quiz.app.service.genquiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.ai.lms.config.CorrelationIdFilter;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
import edu.ai.lms.core.quiz.app.port.in.command.GenerateQuizCommand;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.ScheduleQuizGenerationJobPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizGenerationProcessingService {

    private final LoadQuizGenerationJobPort loadJobPort;
    private final SaveQuizGenerationJobPort saveJobPort;
    private final ScheduleQuizGenerationJobPort schedulerPort;
    private final SseQuizGenerationService sseService;
    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper;
    private final QuizGenerationTimingLogger timingLogger;
    private final Clock quizGenerationClock;

    @Value("${quiz.generation.webhook.base-url}")
    private String webhookBaseUrl;
    @Value("${quiz.generation.webhook.secret}")
    private String webhookSecret;
    @Value("${quiz.generation.timeout-minutes:20}")
    private long timeoutMinutes;
    @Value("${quiz.generation.max-attempts:3}")
    private int maxAttempts;

    public void process(UUID jobId) {
        QuizGenerationJob beforeClaim = loadJobPort.getById(jobId).orElse(null);
        if (beforeClaim == null || isTerminal(beforeClaim.getStatus())) {
            return;
        }
        if (beforeClaim.getCreatedAt() != null
                && beforeClaim.getCreatedAt().plus(Duration.ofMinutes(timeoutMinutes))
                .isBefore(Instant.now(quizGenerationClock))) {
            markTimedOut(beforeClaim);
            return;
        }
        if (beforeClaim.getStatus() == QuizGenerationStatus.PROCESSING
                && beforeClaim.getLeaseUntil() != null
                && beforeClaim.getLeaseUntil().isBefore(Instant.now(quizGenerationClock))) {
            beforeClaim.setStatus(QuizGenerationStatus.PENDING);
            beforeClaim.setNextAttemptAt(Instant.now(quizGenerationClock));
            saveJobPort.save(beforeClaim);
        }
        if (!saveJobPort.claim(jobId, Instant.now(quizGenerationClock).plus(Duration.ofMinutes(timeoutMinutes)))) {
            return;
        }

        QuizGenerationJob job = loadJobPort.getById(jobId).orElseThrow();
        if (job.getAttemptCount() != null && job.getAttemptCount() == 1) {
            timingLogger.textStarted(job);
        }
        sseService.pushStatus(jobId, QuizGenerationService.toStatusResponse(job));
        try {
            JsonNode snapshot = objectMapper.readTree(job.getRequestSnapshot());
            List<UUID> materialIds = new ArrayList<>();
            snapshot.path("selectedMaterials").forEach(node -> materialIds.add(UUID.fromString(node.path("materialId").asText())));
            AiExamMatrix aiExamMatrix = toAiExamMatrixFromSnapshot(snapshot);
            GenerateQuizCommand.QuestionDefaultsConfiguration questionDefaultsConfiguration =
                    questionDefaultsConfigurationFromSnapshot(snapshot);
            AiGenerationRequest request = new AiGenerationRequest(
                    job.getId(), aiExamMatrix.contractVersion(), job.getCourseId(),
                    snapshot.path("courseTitle").asText(), job.getQuizTitle(),
                    aiExamMatrix.rows(), aiExamMatrix.learningOutcomeIds(), materialIds, job.getPrompt(),
                    questionDefaultsConfiguration,
                    snapshot.path("generationMode").asText("STANDARD"),
                    snapshot.hasNonNull("quickQuestionCount") ? snapshot.path("quickQuestionCount").asInt() : null,
                    Boolean.TRUE.equals(job.getStrictMode()), job.getCodeRunnerPolicy(),
                    job.getIdempotencyKey(), callbackUrl(), progressCallbackUrl(), webhookSecret,
                    job.getAttemptCount());

            WebClient.RequestBodySpec call = aiWebClient.post().uri("/quiz-gen/generate")
                    .header(CorrelationIdFilter.HEADER_NAME, job.getId().toString())
                    .contentType(MediaType.APPLICATION_JSON);
            call.bodyValue(request).retrieve().toBodilessEntity()
                    .block(Duration.ofMinutes(timeoutMinutes));

            QuizGenerationJob current = loadJobPort.getById(jobId).orElseThrow();
            if (current.getStatus() == QuizGenerationStatus.PROCESSING
                    && (current.getPhase() == null || current.getPhase() == QuizGenerationJob.Phase.TEXT_GENERATION)) {
                retryOrFail(current, "CALLBACK_MISSING", "AI service returned without delivering a terminal webhook");
            }
        } catch (Exception ex) {
            QuizGenerationJob current = loadJobPort.getById(jobId).orElse(job);
            if (!isTerminal(current.getStatus())) {
                retryOrFail(current, "AI_SERVICE_ERROR", safeMessage(ex));
            }
        }
    }

    private void retryOrFail(QuizGenerationJob job, String code, String message) {
        log.warn("Quiz generation dispatch {} failed with {}: {}", job.getId(), code, message);
        job.setLeaseUntil(null);
        job.setErrorCode(code);
        boolean willRetry = job.getAttemptCount() != null && job.getAttemptCount() < maxAttempts;
        job.setErrorMessage(technicalErrorMessage(message, code));
        if (willRetry) {
            long delaySeconds = Math.min(60, 1L << Math.max(1, job.getAttemptCount()));
            job.setStatus(QuizGenerationStatus.PENDING);
            job.setNextAttemptAt(Instant.now(quizGenerationClock).plusSeconds(delaySeconds));
            QuizGenerationJob saved = saveJobPort.save(job);
            schedulerPort.schedule(saved.getId(), saved.getNextAttemptAt());
        } else {
            job.setStatus(QuizGenerationStatus.FAILED);
            Instant completedAt = Instant.now(quizGenerationClock);
            job.setCompletedAt(completedAt);
            QuizGenerationJob saved = saveJobPort.save(job);
            timingLogger.textTerminal(saved, "failed", code, completedAt);
            timingLogger.quizTerminal(saved, null, completedAt);
            sseService.pushStatus(saved.getId(), QuizGenerationService.toStatusResponse(saved));
        }
    }

    private void markTimedOut(QuizGenerationJob job) {
        job.setStatus(QuizGenerationStatus.TIMED_OUT);
        job.setLeaseUntil(null);
        job.setErrorCode("GENERATION_TIMEOUT");
        job.setErrorMessage("Quiz generation exceeded its deadline");
        Instant completedAt = Instant.now(quizGenerationClock);
        job.setCompletedAt(completedAt);
        QuizGenerationJob saved = saveJobPort.save(job);
        timingLogger.textTerminal(saved, "timed_out", job.getErrorCode(), completedAt);
        timingLogger.quizTerminal(saved, null, completedAt);
        sseService.pushStatus(saved.getId(), QuizGenerationService.toStatusResponse(saved));
    }

    private boolean isTerminal(QuizGenerationStatus status) {
        return status == QuizGenerationStatus.COMPLETED || status == QuizGenerationStatus.FAILED
                || status == QuizGenerationStatus.TIMED_OUT;
    }

    private String callbackUrl() {
        return webhookBaseUrl.replaceAll("/+$", "") + "/api/internal/webhooks/quiz-generation";
    }

    private String progressCallbackUrl() {
        return callbackUrl() + "/progress";
    }

    private String safeMessage(Exception ex) {
        return technicalErrorMessage(ex.getMessage(), ex.getClass().getSimpleName());
    }

    private String technicalErrorMessage(String value, String fallback) {
        String message = value == null ? "" : value.trim();
        if (message.isEmpty() || "no message available".equalsIgnoreCase(message)) message = fallback;
        message = message.replaceAll("(?i)(authorization\\s*[:=]\\s*)([^\\s,;]+)", "$1[REDACTED]");
        message = message.replaceAll("(?i)(bearer\\s+)([^\\s,;]+)", "$1[REDACTED]");
        message = message.replaceAll("(?i)((?:token|secret|password|api[_-]?key)\\s*[:=]\\s*)([^\\s,;]+)", "$1[REDACTED]");
        return message.substring(0, Math.min(1000, message.length()));
    }

    AiExamMatrix toAiExamMatrix(JsonNode snapshotRows) {
        ArrayNode rows = objectMapper.createArrayNode();
        List<UUID> learningOutcomeIds = new ArrayList<>();
        snapshotRows.forEach(source -> {
            ObjectNode row = rows.addObject();
            row.put("clo", source.path("clo").asText());
            row.put("chapter", source.path("chapter").asText());
            row.put("topic", source.path("topic").asText());
            row.put("hieu", source.path("hieu").asInt());
            row.put("van_dung", source.path("van_dung").asInt());
            row.put("vdc", source.path("vdc").asInt());
            learningOutcomeIds.add(UUID.fromString(source.path("learningOutcomeId").asText()));
        });
        return new AiExamMatrix(1, rows, List.copyOf(learningOutcomeIds));
    }

    AiExamMatrix toAiExamMatrixFromSnapshot(JsonNode snapshot) {
        int snapshotVersion = snapshot.path("snapshotVersion").asInt(1);
        if (snapshotVersion < 2) {
            return toAiExamMatrix(snapshot.path("examMatrix"));
        }
        ArrayNode rows = objectMapper.createArrayNode();
        java.util.LinkedHashSet<UUID> learningOutcomeIds = new java.util.LinkedHashSet<>();
        snapshot.path("examMatrix").forEach(source -> {
            rows.add(source.deepCopy());
            source.path("learningOutcomes").forEach(outcome ->
                    learningOutcomeIds.add(UUID.fromString(outcome.path("id").asText())));
        });
        return new AiExamMatrix(snapshotVersion, rows, List.copyOf(learningOutcomeIds));
    }

    GenerateQuizCommand.QuestionDefaultsConfiguration questionDefaultsConfigurationFromSnapshot(
            JsonNode snapshot) {
        JsonNode multipleChoice = snapshot.path("questionDefaultsConfiguration").path("multipleChoice");
        return new GenerateQuizCommand.QuestionDefaultsConfiguration(
                new GenerateQuizCommand.MultipleChoiceDefaults(
                        multipleChoice.path("answerOptionCount").asInt(4),
                        multipleChoice.path("allowMultipleCorrectAnswers").asBoolean(false),
                        multipleChoice.path("requireAnswerExplanation").asBoolean(true)));
    }

    record AiExamMatrix(int contractVersion, JsonNode rows, List<UUID> learningOutcomeIds) {
    }

    public record AiGenerationRequest(
            UUID jobId,
            int contractVersion,
            @JsonProperty("course_id") UUID courseId,
            String courseTitle,
            String quizTitle,
            JsonNode examMatrix,
            List<UUID> learningOutcomeIds,
            @JsonProperty("material_ids") List<UUID> selectedMaterialIds,
            String teacherInstruction,
            GenerateQuizCommand.QuestionDefaultsConfiguration questionDefaultsConfiguration,
            String generationMode,
            Integer quickQuestionCount,
            boolean strictMode,
            String codeRunnerPolicy,
            String idempotencyKey,
            String webhookUrl,
            String progressWebhookUrl,
            String webhookSecret,
            Integer attempt) {
    }
}
