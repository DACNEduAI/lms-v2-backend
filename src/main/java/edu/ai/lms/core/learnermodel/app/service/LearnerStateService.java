package edu.ai.lms.core.learnermodel.app.service;

import edu.ai.lms.core.attempt.api.AttemptFinishedEvent;
import edu.ai.lms.core.learnermodel.app.domain.LearnerModelPredictionResult;
import edu.ai.lms.core.learnermodel.app.domain.LearnerModelType;
import edu.ai.lms.core.learnermodel.app.domain.LearnerState;
import edu.ai.lms.core.learnermodel.app.domain.LearnerStateOutcome;
import edu.ai.lms.core.learnermodel.app.domain.LearnerStateOutcomeStatus;
import edu.ai.lms.core.learnermodel.app.domain.LearningOutcomePrediction;
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelAccessDeniedException;
import edu.ai.lms.core.learnermodel.app.port.in.usecase.GetLearnerStateHistoryUseCase;
import edu.ai.lms.core.learnermodel.app.port.in.usecase.GetLatestLearnerStateUseCase;
import edu.ai.lms.core.learnermodel.app.port.out.LearnerStatePort;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerActorPort;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerCoursePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class LearnerStateService implements GetLearnerStateHistoryUseCase, GetLatestLearnerStateUseCase {
    private final LoadLearnerActorPort actorPort;
    private final LoadLearnerCoursePort coursePort;
    private final LearnerModelPredictionCalculator calculator;
    private final LearnerStatePort statePort;

    /**
     * This is a regular synchronous event listener. It deliberately executes in the
     * caller's transaction so its model history can include the just-finished attempt.
     */
    @EventListener
    public void onAttemptFinished(AttemptFinishedEvent event) {
        if (statePort.existsForAttempt(event.attemptId())) {
            return;
        }
        try {
            List<LearnerModelPredictionResult> predictions = new ArrayList<>();
            for (LearnerModelType model : LearnerModelType.values()) {
                predictions.add(calculator.predictAllThroughAttempt(
                        event.studentId(), event.courseId(), event.attemptId(), event.submittedAt(), model));
            }
            LearnerState state = new LearnerState(
                    UUID.randomUUID(), event.studentId(), event.courseId(), event.attemptId(), LocalDateTime.now(),
                    averageAll(predictions));
            if (!statePort.saveIfAbsent(state)) {
                log.debug("Learner state already exists for attempt {}", event.attemptId());
            }
        } catch (RuntimeException exception) {
            // Snapshot failure must never turn an already finished quiz back into a failed submission.
            log.warn("Could not calculate learner state for finished attempt {}", event.attemptId(), exception);
        }
    }

    @Override
    public List<LearnerState> getHistory(UUID courseId) {
        UUID studentId = requireEnrolledStudent(courseId);
        return statePort.findByStudentAndCourse(studentId, courseId);
    }

    @Override
    public Optional<LearnerState> getLatest(UUID courseId) {
        return statePort.findLatestByStudentAndCourse(requireEnrolledStudent(courseId), courseId);
    }

    private UUID requireEnrolledStudent(UUID courseId) {
        UUID studentId = actorPort.requireStudentId();
        if (!coursePort.isStudentEnrolled(studentId, courseId)) {
            throw new LearnerModelAccessDeniedException("Student is not enrolled in this course");
        }
        return studentId;
    }

    private List<LearnerStateOutcome> averageAll(List<LearnerModelPredictionResult> modelResults) {
        if (modelResults.isEmpty()) {
            return List.of();
        }
        LearnerModelPredictionResult aktPyktResult = modelResults.stream()
                .filter(result -> result.model() == LearnerModelType.AKT_PYKT)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("AKT-pyKT prediction result is required"));
        return modelResults.getFirst().predictions().stream().map(reference -> {
            List<LearningOutcomePrediction> perModel = modelResults.stream()
                    .map(result -> result.predictions().stream()
                            .filter(item -> item.learningOutcomeId().equals(reference.learningOutcomeId()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException(
                                    "Model result omitted learning outcome " + reference.learningOutcomeId())))
                    .toList();
            boolean allNumeric = perModel.stream().allMatch(item -> item.probability() != null);
            LearningOutcomePrediction aktPykt = aktPyktResult.predictions().stream()
                    .filter(item -> item.learningOutcomeId().equals(reference.learningOutcomeId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "AKT-pyKT result omitted learning outcome " + reference.learningOutcomeId()));
            if (!allNumeric) {
                return new LearnerStateOutcome(reference.learningOutcomeId(), null,
                        LearnerStateOutcomeStatus.INSUFFICIENT_PREDICTION,
                        aktPykt.probability(), aktPykt.status());
            }
            double mean = perModel.stream().mapToDouble(LearningOutcomePrediction::probability).average()
                    .orElseThrow();
            return new LearnerStateOutcome(reference.learningOutcomeId(), mean, LearnerStateOutcomeStatus.PREDICTED,
                    aktPykt.probability(), aktPykt.status());
        }).toList();
    }
}
