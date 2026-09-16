package edu.ai.lms.core.learnermodel.adapter.out.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.ai.lms.core.learnermodel.app.domain.LearnerModelType;
import edu.ai.lms.core.learnermodel.app.domain.LearnerQuizHistory;
import edu.ai.lms.core.learnermodel.app.domain.LearningOutcomeTarget;
import edu.ai.lms.core.learnermodel.app.domain.ModelOutcomePrediction;
import edu.ai.lms.core.learnermodel.app.domain.PredictionStatus;
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelUpstreamException;
import edu.ai.lms.core.learnermodel.app.port.out.LearnerModelInferencePort;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Component
class LearnerModelWebClientAdapter implements LearnerModelInferencePort {
    private static final String CORRELATION_HEADER = "X-Correlation-ID";

    private final WebClient client;
    private final Duration timeout;

    LearnerModelWebClientAdapter(
            WebClient.Builder builder,
            @Value("${learner-model.service.base-url:http://localhost:8081}") String baseUrl,
            @Value("${learner-model.service.response-timeout:30s}") Duration timeout) {
        this.client = builder.baseUrl(baseUrl).build();
        this.timeout = timeout;
    }

    @Override
    public List<ModelOutcomePrediction> predict(
            LearnerModelType model,
            UUID studentId,
            List<LearnerQuizHistory> history,
            List<LearningOutcomeTarget> targets) {
        UpstreamRequest payload = new UpstreamRequest(
                studentId.toString(),
                history.stream().map(item -> toHistory(model, item)).toList(),
                targets.stream().map(target -> model.toModelId(target.ktLearningOutcomeId())).toList());
        String path = model.isAkt()
                ? "/api/v1/" + model.wireName() + "/predict-learning-outcomes"
                : "/api/v1/" + model.wireName() + "/predict";
        try {
            WebClient.RequestBodySpec request = client.post().uri(path)
                    .contentType(MediaType.APPLICATION_JSON);
            String correlationId = MDC.get("correlation_id");
            if (correlationId != null && !correlationId.isBlank()) {
                request.header(CORRELATION_HEADER, correlationId);
            }
            UpstreamResponse response = request.bodyValue(payload)
                    .retrieve()
                    .bodyToMono(UpstreamResponse.class)
                    .timeout(timeout)
                    .block();
            return validateAndMap(model, studentId, targets, response);
        } catch (WebClientResponseException exception) {
            int status = exception.getStatusCode().value();
            LearnerModelUpstreamException.Kind kind = status == 408 || status == 429
                    || exception.getStatusCode().is5xxServerError()
                    ? LearnerModelUpstreamException.Kind.UNAVAILABLE
                    : LearnerModelUpstreamException.Kind.CONTRACT;
            throw new LearnerModelUpstreamException(
                    kind,
                    "Learner-model service returned HTTP " + exception.getStatusCode().value(),
                    exception);
        } catch (WebClientRequestException exception) {
            throw new LearnerModelUpstreamException(
                    LearnerModelUpstreamException.Kind.UNAVAILABLE,
                    "Learner-model service is unavailable",
                    exception);
        } catch (LearnerModelUpstreamException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (isTimeout(exception)) {
                throw new LearnerModelUpstreamException(
                        LearnerModelUpstreamException.Kind.UNAVAILABLE,
                        "Learner-model service timed out",
                        exception);
            }
            throw new LearnerModelUpstreamException(
                    LearnerModelUpstreamException.Kind.BAD_RESPONSE,
                    "Learner-model response could not be read",
                    exception);
        }
    }

    private UpstreamQuizHistory toHistory(LearnerModelType model, LearnerQuizHistory history) {
        return new UpstreamQuizHistory(
                history.quizId().toString(),
                history.attemptId().toString(),
                history.submittedAt(),
                history.interactions().stream()
                        .map(interaction -> new UpstreamInteraction(
                                model.isAkt() ? model.toModelId(interaction.ktQuestionId()) : null,
                                model.toModelId(interaction.ktLearningOutcomeId()),
                                interaction.correct()))
                        .toList());
    }

    private List<ModelOutcomePrediction> validateAndMap(
            LearnerModelType model,
            UUID studentId,
            List<LearningOutcomeTarget> targets,
            UpstreamResponse response) {
        if (response == null
                || !model.wireName().equals(response.model())
                || !studentId.toString().equals(response.studentId())
                || response.predictions() == null) {
            throw new LearnerModelUpstreamException(
                    LearnerModelUpstreamException.Kind.BAD_RESPONSE,
                    "Learner-model response did not match the request",
                    null);
        }
        Set<Long> expectedIds = targets.stream()
                .map(LearningOutcomeTarget::ktLearningOutcomeId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> returnedIds = new HashSet<>();
        List<ModelOutcomePrediction> mapped = response.predictions().stream().map(prediction -> {
            PredictionStatus status = prediction.status() == null
                    ? PredictionStatus.PREDICTED
                    : PredictionStatus.valueOf(prediction.status());
            List<Long> questions = prediction.questionIds() == null
                    ? List.of()
                    : prediction.questionIds().stream().map(model::toCanonicalId).toList();
            if (prediction.tagId() == null) {
                throw new LearnerModelUpstreamException(
                        LearnerModelUpstreamException.Kind.BAD_RESPONSE,
                        "Learner-model response omitted a prediction tag",
                        null);
            }
            long canonicalTagId = model.toCanonicalId(prediction.tagId());
            if (!returnedIds.add(canonicalTagId)
                    || !expectedIds.contains(canonicalTagId)
                    || !validStatusAndEvidence(model, status, questions)
                    || !validProbability(status, prediction.correctnessProbability())) {
                throw new LearnerModelUpstreamException(
                        LearnerModelUpstreamException.Kind.BAD_RESPONSE,
                        "Learner-model response contained an invalid prediction",
                        null);
            }
            return new ModelOutcomePrediction(
                    canonicalTagId,
                    prediction.correctnessProbability(),
                    status,
                    questions);
        }).toList();
        if (!returnedIds.equals(expectedIds)) {
            throw new LearnerModelUpstreamException(
                    LearnerModelUpstreamException.Kind.BAD_RESPONSE,
                    "Learner-model response omitted a requested learning outcome",
                    null);
        }
        return mapped;
    }

    private boolean validProbability(PredictionStatus status, Double probability) {
        if (status == PredictionStatus.PREDICTED) {
            return probability != null && Double.isFinite(probability)
                    && probability >= 0.0d && probability <= 1.0d;
        }
        return probability == null;
    }

    private boolean validStatusAndEvidence(
            LearnerModelType model,
            PredictionStatus status,
            List<Long> questions) {
        if (!model.isAkt()) {
            return status == PredictionStatus.PREDICTED && questions.isEmpty();
        }
        if (status == PredictionStatus.PREDICTED) {
            return !questions.isEmpty();
        }
        return status == PredictionStatus.INSUFFICIENT_OUTCOME_HISTORY && questions.isEmpty();
    }

    private boolean isTimeout(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof TimeoutException
                    || current.getClass().getName().equals("io.netty.handler.timeout.ReadTimeoutException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record UpstreamRequest(
            @JsonProperty("student_id") String studentId,
            @JsonProperty("quiz_histories") List<UpstreamQuizHistory> quizHistories,
            @JsonProperty("target_tag_ids") List<Long> targetTagIds) {
    }

    private record UpstreamQuizHistory(
            @JsonProperty("quiz_id") String quizId,
            @JsonProperty("attempt_id") String attemptId,
            @JsonProperty("submitted_at") OffsetDateTime submittedAt,
            List<UpstreamInteraction> interactions) {
    }

    private record UpstreamInteraction(
            @JsonProperty("question_id") @JsonInclude(JsonInclude.Include.NON_NULL) Long questionId,
            @JsonProperty("tag_id") long tagId,
            @JsonProperty("is_correct") boolean correct) {
    }

    private record UpstreamResponse(
            String model,
            @JsonProperty("student_id") String studentId,
            List<UpstreamPrediction> predictions) {
    }

    private record UpstreamPrediction(
            @JsonProperty("tag_id") Long tagId,
            @JsonProperty("question_ids") List<Long> questionIds,
            @JsonProperty("correctness_probability") Double correctnessProbability,
            String status) {
    }
}
