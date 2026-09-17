package edu.ai.lms.core.learnermodel.app.service;

import edu.ai.lms.core.learnermodel.app.domain.LearnerInteraction;
import edu.ai.lms.core.learnermodel.app.domain.LearnerModelPredictionResult;
import edu.ai.lms.core.learnermodel.app.domain.LearnerModelType;
import edu.ai.lms.core.learnermodel.app.domain.LearnerQuizHistory;
import edu.ai.lms.core.learnermodel.app.domain.LearningOutcomePrediction;
import edu.ai.lms.core.learnermodel.app.domain.LearningOutcomeTarget;
import edu.ai.lms.core.learnermodel.app.domain.ModelOutcomePrediction;
import edu.ai.lms.core.learnermodel.app.domain.PredictionStatus;
import edu.ai.lms.core.learnermodel.app.exception.AmbiguousLearningOutcomeHistoryException;
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelUpstreamException;
import edu.ai.lms.core.learnermodel.app.port.out.LearnerModelInferencePort;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerCoursePort;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerHistoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Shared prediction core for both authenticated HTTP requests and internal snapshots. */
@Component
@RequiredArgsConstructor
class LearnerModelPredictionCalculator {
    private final LoadLearnerCoursePort coursePort;
    private final LoadLearnerHistoryPort historyPort;
    private final LearnerModelInferencePort inferencePort;

    LearnerModelPredictionResult predictAll(UUID studentId, UUID courseId, LearnerModelType model) {
        return predict(studentId, courseId, model, coursePort.findLearningOutcomes(courseId),
                historyPort.load(studentId, courseId));
    }

    LearnerModelPredictionResult predictAllThroughAttempt(
            UUID studentId,
            UUID courseId,
            UUID attemptId,
            LocalDateTime submittedAt,
            LearnerModelType model) {
        return predict(studentId, courseId, model, coursePort.findLearningOutcomes(courseId),
                historyPort.loadThroughAttempt(studentId, courseId, attemptId, submittedAt));
    }

    LearnerModelPredictionResult predictSingle(
            UUID studentId,
            UUID courseId,
            LearnerModelType model,
            LearningOutcomeTarget target) {
        return predict(studentId, courseId, model, List.of(target), historyPort.load(studentId, courseId));
    }

    private LearnerModelPredictionResult predict(
            UUID studentId,
            UUID courseId,
            LearnerModelType model,
            List<LearningOutcomeTarget> targets,
            List<LearnerQuizHistory> rawHistory) {
        rawHistory.stream()
                .flatMap(history -> history.interactions().stream())
                .filter(interaction -> interaction.learningOutcomeMappingCount() > 1)
                .findFirst()
                .ifPresent(interaction -> {
                    throw new AmbiguousLearningOutcomeHistoryException(
                            "Question " + interaction.questionId()
                                    + " was linked to multiple learning outcomes at submission time");
                });

        List<LearnerQuizHistory> history = rawHistory.stream()
                .map(item -> new LearnerQuizHistory(
                        item.quizId(), item.attemptId(), item.submittedAt(), item.interactions().stream()
                                .filter(interaction -> interaction.learningOutcomeMappingCount() == 1)
                                .toList()))
                .filter(item -> !item.interactions().isEmpty())
                .toList();
        int historyCount = history.stream().mapToInt(item -> item.interactions().size()).sum();

        List<ModelOutcomePrediction> modelPredictions;
        if (targets.isEmpty()) {
            modelPredictions = List.of();
        } else if (historyCount == 0) {
            PredictionStatus status = model.isAkt()
                    ? PredictionStatus.INSUFFICIENT_OUTCOME_HISTORY
                    : PredictionStatus.INSUFFICIENT_HISTORY;
            modelPredictions = targets.stream()
                    .map(target -> new ModelOutcomePrediction(target.ktLearningOutcomeId(), null, status, List.of()))
                    .toList();
        } else {
            modelPredictions = inferencePort.predict(model, studentId, history, targets);
        }

        Map<Long, ModelOutcomePrediction> predictionsByOutcome = new LinkedHashMap<>();
        modelPredictions.forEach(prediction -> predictionsByOutcome.put(prediction.ktLearningOutcomeId(), prediction));
        Map<Long, UUID> questionsByKtId = new LinkedHashMap<>();
        history.stream().flatMap(item -> item.interactions().stream()).forEach(interaction ->
                questionsByKtId.putIfAbsent(interaction.ktQuestionId(), interaction.questionId()));

        List<LearningOutcomePrediction> predictions = targets.stream().map(target -> {
            ModelOutcomePrediction modelPrediction = predictionsByOutcome.get(target.ktLearningOutcomeId());
            if (modelPrediction == null) {
                throw new LearnerModelUpstreamException(LearnerModelUpstreamException.Kind.BAD_RESPONSE,
                        "Learner-model response omitted learning outcome " + target.ktLearningOutcomeId(), null);
            }
            List<UUID> basisQuestions = modelPrediction.basisKtQuestionIds().stream().map(questionId -> {
                UUID mapped = questionsByKtId.get(questionId);
                if (mapped == null) {
                    throw new LearnerModelUpstreamException(LearnerModelUpstreamException.Kind.BAD_RESPONSE,
                            "Learner-model response referenced unknown question " + questionId, null);
                }
                return mapped;
            }).toList();
            return new LearningOutcomePrediction(
                    target.id(), modelPrediction.probability(), modelPrediction.status(), basisQuestions);
        }).toList();

        return new LearnerModelPredictionResult(courseId, studentId, model, historyCount, predictions);
    }
}
