package edu.ai.lms.core.learnermodel.app.service;

import edu.ai.lms.core.learnermodel.app.domain.LearnerInteraction;
import edu.ai.lms.core.learnermodel.app.domain.LearnerModelType;
import edu.ai.lms.core.learnermodel.app.domain.LearnerQuizHistory;
import edu.ai.lms.core.learnermodel.app.domain.LearningOutcomeTarget;
import edu.ai.lms.core.learnermodel.app.domain.ModelOutcomePrediction;
import edu.ai.lms.core.learnermodel.app.domain.PredictionStatus;
import edu.ai.lms.core.learnermodel.app.exception.AmbiguousLearningOutcomeHistoryException;
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelNotFoundException;
import edu.ai.lms.core.learnermodel.app.port.out.LearnerModelInferencePort;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerActorPort;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerCoursePort;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerHistoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredictLearnerModelServiceUT {
    @Mock private LoadLearnerCoursePort coursePort;
    @Mock private LoadLearnerHistoryPort historyPort;
    @Mock private LearnerModelInferencePort inferencePort;
    @InjectMocks private LearnerModelPredictionCalculator calculator;

    @Test
    void returnsInsufficientHistoryForEveryDktTargetWithoutCallingUpstream() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        var targets = List.of(target(1), target(2));
        when(coursePort.findLearningOutcomes(courseId)).thenReturn(targets);
        when(historyPort.load(studentId, courseId)).thenReturn(List.of());

        var result = calculator.predictAll(studentId, courseId, LearnerModelType.DKT_CUSTOM);

        assertThat(result.predictions()).allMatch(item ->
                item.status() == PredictionStatus.INSUFFICIENT_HISTORY
                        && item.probability() == null);
        verify(inferencePort, never()).predict(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void mapsAktEvidenceIdsBackToQuestionUuids() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        LearningOutcomeTarget target = target(5);
        LearnerQuizHistory history = history(questionId, 9, target, 1);
        when(historyPort.load(studentId, courseId)).thenReturn(List.of(history));
        when(inferencePort.predict(
                LearnerModelType.AKT_CUSTOM, studentId, List.of(history), List.of(target)))
                .thenReturn(List.of(new ModelOutcomePrediction(
                        5, 0.75, PredictionStatus.PREDICTED, List.of(9L))));

        var result = calculator.predictSingle(studentId, courseId, LearnerModelType.AKT_CUSTOM, target);

        assertThat(result.historyInteractionCount()).isEqualTo(1);
        assertThat(result.predictions().getFirst().probability()).isEqualTo(0.75);
        assertThat(result.predictions().getFirst().basisQuestionIds()).containsExactly(questionId);
    }

    @Test
    void rejectsQuestionSnapshotWithMultipleLearningOutcomes() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        LearningOutcomeTarget target = target(3);
        LearnerQuizHistory history = history(UUID.randomUUID(), 8, target, 2);
        when(coursePort.findLearningOutcomes(courseId)).thenReturn(List.of(target));
        when(historyPort.load(studentId, courseId)).thenReturn(List.of(history));

        assertThatThrownBy(() -> calculator.predictAll(studentId, courseId, LearnerModelType.AKT_PYKT))
                .isInstanceOf(AmbiguousLearningOutcomeHistoryException.class);
    }

    @Test
    void convertsCanonicalIdentifiersForEveryModel() {
        assertThat(LearnerModelType.AKT_CUSTOM.toModelId(12)).isEqualTo(12);
        assertThat(LearnerModelType.AKT_PYKT.toModelId(12)).isEqualTo(11);
        assertThat(LearnerModelType.DKT_CUSTOM.toModelId(12)).isEqualTo(11);
        assertThat(LearnerModelType.DKT_PYKT.toModelId(12)).isEqualTo(11);
        assertThat(LearnerModelType.AKT_PYKT.toCanonicalId(11)).isEqualTo(12);
    }

    @Test
    void authenticatedPredictionRejectsAStudentWhoIsNotEnrolled() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        LoadLearnerActorPort actor = org.mockito.Mockito.mock(LoadLearnerActorPort.class);
        LearnerModelPredictionCalculator delegate = org.mockito.Mockito.mock(LearnerModelPredictionCalculator.class);
        PredictLearnerModelService service = new PredictLearnerModelService(actor, coursePort, delegate);
        when(actor.requireStudentId()).thenReturn(studentId);
        when(coursePort.isStudentEnrolled(studentId, courseId)).thenReturn(false);

        assertThatThrownBy(() -> service.predictAll(new edu.ai.lms.core.learnermodel.app.port.in.command.PredictLearnerModelCommand(
                courseId, null, LearnerModelType.DKT_CUSTOM)))
                .isInstanceOf(edu.ai.lms.core.learnermodel.app.exception.LearnerModelAccessDeniedException.class);

        verify(delegate, never()).predictAll(studentId, courseId, LearnerModelType.DKT_CUSTOM);
    }

    @Test
    void returnsNotFoundWhenRequestedOutcomeIsOutsideTheCourse() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID outcomeId = UUID.randomUUID();
        when(coursePort.findLearningOutcome(courseId, outcomeId)).thenReturn(Optional.empty());
        LoadLearnerActorPort actor = org.mockito.Mockito.mock(LoadLearnerActorPort.class);
        LearnerModelPredictionCalculator delegate = org.mockito.Mockito.mock(LearnerModelPredictionCalculator.class);
        PredictLearnerModelService service = new PredictLearnerModelService(actor, coursePort, delegate);
        when(actor.requireStudentId()).thenReturn(studentId);
        when(coursePort.isStudentEnrolled(studentId, courseId)).thenReturn(true);

        assertThatThrownBy(() -> service.predictSingle(new edu.ai.lms.core.learnermodel.app.port.in.command.PredictLearnerModelCommand(
                courseId, outcomeId, LearnerModelType.AKT_CUSTOM)))
                .isInstanceOf(LearnerModelNotFoundException.class);

        org.mockito.Mockito.verifyNoInteractions(delegate);
    }

    private LearningOutcomeTarget target(long id) {
        return new LearningOutcomeTarget(UUID.randomUUID(), id);
    }

    private LearnerQuizHistory history(
            UUID questionId,
            long ktQuestionId,
            LearningOutcomeTarget target,
            int mappingCount) {
        return new LearnerQuizHistory(
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-09-16T10:00:00+07:00"),
                List.of(new LearnerInteraction(
                        questionId,
                        ktQuestionId,
                        true,
                        mappingCount == 1 ? target.id() : null,
                        mappingCount == 1 ? target.ktLearningOutcomeId() : null,
                        mappingCount)));
    }
}
