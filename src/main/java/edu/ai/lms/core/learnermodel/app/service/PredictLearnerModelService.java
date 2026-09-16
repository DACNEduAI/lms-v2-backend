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
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelAccessDeniedException;
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelNotFoundException;
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelUpstreamException;
import edu.ai.lms.core.learnermodel.app.port.in.command.PredictLearnerModelCommand;
import edu.ai.lms.core.learnermodel.app.port.in.usecase.PredictLearnerModelUseCase;
import edu.ai.lms.core.learnermodel.app.port.out.LearnerModelInferencePort;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerActorPort;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerCoursePort;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerHistoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PredictLearnerModelService implements PredictLearnerModelUseCase {
    private final LoadLearnerActorPort actorPort;
    private final LoadLearnerCoursePort coursePort;
    private final LoadLearnerHistoryPort historyPort;
    private final LearnerModelInferencePort inferencePort;

    @Override
    public LearnerModelPredictionResult predictSingle(PredictLearnerModelCommand command) {
        UUID studentId = requireStudentAccess(command.courseId());
        LearningOutcomeTarget target = coursePort
                .findLearningOutcome(command.courseId(), command.learningOutcomeId())
                .orElseThrow(() -> new LearnerModelNotFoundException(
                        "Learning outcome does not belong to the course"));
        return predict(command, studentId, List.of(target));
    }

    @Override
    public LearnerModelPredictionResult predictAll(PredictLearnerModelCommand command) {
        UUID studentId = requireStudentAccess(command.courseId());
        return predict(command, studentId, coursePort.findLearningOutcomes(command.courseId()));
    }

    private LearnerModelPredictionResult predict(
            PredictLearnerModelCommand command,
            UUID studentId,
            List<LearningOutcomeTarget> targets) {
        List<LearnerQuizHistory> rawHistory = historyPort.load(studentId, command.courseId());
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
                        item.quizId(),
                        item.attemptId(),
                        item.submittedAt(),
                        item.interactions().stream()
                                .filter(interaction -> interaction.learningOutcomeMappingCount() == 1)
                                .toList()))
                .filter(item -> !item.interactions().isEmpty())
                .toList();
        int historyCount = history.stream().mapToInt(item -> item.interactions().size()).sum();

        List<ModelOutcomePrediction> modelPredictions;
        if (targets.isEmpty()) {
            modelPredictions = List.of();
        } else if (historyCount == 0) {
            PredictionStatus status = command.model().isAkt()
                    ? PredictionStatus.INSUFFICIENT_OUTCOME_HISTORY
                    : PredictionStatus.INSUFFICIENT_HISTORY;
            modelPredictions = targets.stream()
                    .map(target -> new ModelOutcomePrediction(
                            target.ktLearningOutcomeId(), null, status, List.of()))
                    .toList();
        } else {
            modelPredictions = inferencePort.predict(
                    command.model(), studentId, history, targets);
        }

        Map<Long, ModelOutcomePrediction> predictionsByOutcome = new LinkedHashMap<>();
        modelPredictions.forEach(prediction ->
                predictionsByOutcome.put(prediction.ktLearningOutcomeId(), prediction));
        Map<Long, UUID> questionsByKtId = new LinkedHashMap<>();
        history.stream().flatMap(item -> item.interactions().stream()).forEach(interaction ->
                questionsByKtId.putIfAbsent(interaction.ktQuestionId(), interaction.questionId()));

        List<LearningOutcomePrediction> predictions = targets.stream().map(target -> {
            ModelOutcomePrediction modelPrediction = predictionsByOutcome.get(target.ktLearningOutcomeId());
            if (modelPrediction == null) {
                throw new LearnerModelUpstreamException(
                        LearnerModelUpstreamException.Kind.BAD_RESPONSE,
                        "Learner-model response omitted learning outcome " + target.ktLearningOutcomeId(),
                        null);
            }
            List<UUID> basisQuestions = modelPrediction.basisKtQuestionIds().stream()
                    .map(questionId -> {
                        UUID mapped = questionsByKtId.get(questionId);
                        if (mapped == null) {
                            throw new LearnerModelUpstreamException(
                                    LearnerModelUpstreamException.Kind.BAD_RESPONSE,
                                    "Learner-model response referenced unknown question " + questionId,
                                    null);
                        }
                        return mapped;
                    })
                    .toList();
            return new LearningOutcomePrediction(
                    target.id(),
                    modelPrediction.probability(),
                    modelPrediction.status(),
                    basisQuestions);
        }).toList();

        return new LearnerModelPredictionResult(
                command.courseId(),
                studentId,
                command.model(),
                historyCount,
                predictions);
    }

    private UUID requireStudentAccess(UUID courseId) {
        UUID studentId = actorPort.requireStudentId();
        if (!coursePort.isStudentEnrolled(studentId, courseId)) {
            throw new LearnerModelAccessDeniedException("Student is not enrolled in this course");
        }
        return studentId;
    }
}
