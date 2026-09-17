package edu.ai.lms.core.learnermodel.app.service;

import edu.ai.lms.core.attempt.api.AttemptFinishedEvent;
import edu.ai.lms.core.learnermodel.app.domain.LearnerModelPredictionResult;
import edu.ai.lms.core.learnermodel.app.domain.LearnerModelType;
import edu.ai.lms.core.learnermodel.app.domain.LearnerState;
import edu.ai.lms.core.learnermodel.app.domain.LearnerStateOutcomeStatus;
import edu.ai.lms.core.learnermodel.app.domain.LearningOutcomePrediction;
import edu.ai.lms.core.learnermodel.app.domain.PredictionStatus;
import edu.ai.lms.core.learnermodel.app.port.out.LearnerStatePort;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerActorPort;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerCoursePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearnerStateServiceUT {
    @Mock private LoadLearnerActorPort actorPort;
    @Mock private LoadLearnerCoursePort coursePort;
    @Mock private LearnerModelPredictionCalculator calculator;
    @Mock private LearnerStatePort statePort;
    @InjectMocks private LearnerStateService service;

    @Test
    void savesTheExactMeanForEveryOutcomeWhenAllFourModelsPredict() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID firstOutcome = UUID.randomUUID();
        UUID secondOutcome = UUID.randomUUID();
        AttemptFinishedEvent event = new AttemptFinishedEvent(
                attemptId, studentId, courseId, LocalDateTime.parse("2026-09-17T10:00:00"));
        when(statePort.existsForAttempt(attemptId)).thenReturn(false);
        stub(event, LearnerModelType.DKT_CUSTOM, firstOutcome, secondOutcome, 0.2, 0.5);
        stub(event, LearnerModelType.DKT_PYKT, firstOutcome, secondOutcome, 0.4, 0.7);
        stub(event, LearnerModelType.AKT_CUSTOM, firstOutcome, secondOutcome, 0.6, 0.9);
        stub(event, LearnerModelType.AKT_PYKT, firstOutcome, secondOutcome, 0.8, 0.3);
        when(statePort.saveIfAbsent(any())).thenReturn(true);

        service.onAttemptFinished(event);

        ArgumentCaptor<LearnerState> state = ArgumentCaptor.forClass(LearnerState.class);
        verify(statePort).saveIfAbsent(state.capture());
        assertThat(state.getValue().attemptId()).isEqualTo(attemptId);
        assertThat(state.getValue().outcomes()).extracting(item -> item.probability())
                .containsExactly(0.5, 0.6);
        assertThat(state.getValue().outcomes()).allMatch(item -> item.status() == LearnerStateOutcomeStatus.PREDICTED);
    }

    @Test
    void persistsNullWhenAnyModelHasInsufficientPrediction() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID outcome = UUID.randomUUID();
        AttemptFinishedEvent event = new AttemptFinishedEvent(attemptId, studentId, courseId, LocalDateTime.now());
        when(statePort.existsForAttempt(attemptId)).thenReturn(false);
        stub(event, LearnerModelType.DKT_CUSTOM, outcome, null, 0.5, 0.0);
        stub(event, LearnerModelType.DKT_PYKT, outcome, null, 0.4, 0.0);
        stub(event, LearnerModelType.AKT_CUSTOM, outcome, null, 0.6, 0.0);
        stub(event, LearnerModelType.AKT_PYKT, outcome, null, 0.8, 0.0);
        when(statePort.saveIfAbsent(any())).thenReturn(true);

        service.onAttemptFinished(event);

        ArgumentCaptor<LearnerState> state = ArgumentCaptor.forClass(LearnerState.class);
        verify(statePort).saveIfAbsent(state.capture());
        assertThat(state.getValue().outcomes()).singleElement().satisfies(item -> {
            assertThat(item.probability()).isNull();
            assertThat(item.status()).isEqualTo(LearnerStateOutcomeStatus.INSUFFICIENT_PREDICTION);
        });
    }

    @Test
    void leavesNoSnapshotWhenInferenceFails() {
        AttemptFinishedEvent event = new AttemptFinishedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now());
        when(statePort.existsForAttempt(event.attemptId())).thenReturn(false);
        when(calculator.predictAllThroughAttempt(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("service down"));

        service.onAttemptFinished(event);

        verify(statePort, never()).saveIfAbsent(any());
    }

    @Test
    void ignoresAnAlreadyPersistedAttemptState() {
        AttemptFinishedEvent event = new AttemptFinishedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now());
        when(statePort.existsForAttempt(event.attemptId())).thenReturn(true);

        service.onAttemptFinished(event);

        verify(calculator, never()).predictAllThroughAttempt(any(), any(), any(), any(), any());
    }

    @Test
    void returnsThePersistedLatestStateForTheAuthenticatedEnrolledStudent() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        LearnerState expected = new LearnerState(
                UUID.randomUUID(), studentId, courseId, UUID.randomUUID(), LocalDateTime.now(), List.of());
        when(actorPort.requireStudentId()).thenReturn(studentId);
        when(coursePort.isStudentEnrolled(studentId, courseId)).thenReturn(true);
        when(statePort.findLatestByStudentAndCourse(studentId, courseId)).thenReturn(Optional.of(expected));

        assertThat(service.getLatest(courseId)).contains(expected);

        verify(statePort).findLatestByStudentAndCourse(studentId, courseId);
    }

    private void stub(
            AttemptFinishedEvent event,
            LearnerModelType model,
            UUID firstOutcome,
            UUID secondOutcome,
            Double firstProbability,
            double secondProbability) {
        List<LearningOutcomePrediction> predictions = secondOutcome == null
                ? List.of(prediction(firstOutcome, firstProbability))
                : List.of(prediction(firstOutcome, firstProbability), prediction(secondOutcome, secondProbability));
        when(calculator.predictAllThroughAttempt(
                eq(event.studentId()), eq(event.courseId()), eq(event.attemptId()), eq(event.submittedAt()), eq(model)))
                .thenReturn(new LearnerModelPredictionResult(
                        event.courseId(), event.studentId(), model, 1, predictions));
    }

    private LearningOutcomePrediction prediction(UUID outcomeId, Double probability) {
        return new LearningOutcomePrediction(outcomeId, probability,
                probability == null ? PredictionStatus.INSUFFICIENT_OUTCOME_HISTORY : PredictionStatus.PREDICTED,
                List.of());
    }
}
