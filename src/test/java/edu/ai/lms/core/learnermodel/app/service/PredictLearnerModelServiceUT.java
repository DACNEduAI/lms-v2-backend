package edu.ai.lms.core.learnermodel.app.service;

import edu.ai.lms.core.learnermodel.app.domain.LearnerInteraction;
import edu.ai.lms.core.learnermodel.app.domain.LearnerModelType;
import edu.ai.lms.core.learnermodel.app.domain.LearnerQuizHistory;
import edu.ai.lms.core.learnermodel.app.domain.LearningOutcomeTarget;
import edu.ai.lms.core.learnermodel.app.domain.ModelOutcomePrediction;
import edu.ai.lms.core.learnermodel.app.domain.PredictionStatus;
import edu.ai.lms.core.learnermodel.app.exception.AmbiguousLearningOutcomeHistoryException;
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelAccessDeniedException;
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelNotFoundException;
import edu.ai.lms.core.learnermodel.app.port.in.command.PredictLearnerModelCommand;
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
    @Mock private LoadLearnerActorPort actorPort;
    @Mock private LoadLearnerCoursePort coursePort;
    @Mock private LoadLearnerHistoryPort historyPort;
    @Mock private LearnerModelInferencePort inferencePort;
    @InjectMocks private PredictLearnerModelService service;

    @Test
    void returnsInsufficientHistoryForEveryDktTargetWithoutCallingUpstream() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        var targets = List.of(target(1), target(2));
        when(actorPort.requireStudentId()).thenReturn(studentId);
        when(coursePort.isStudentEnrolled(studentId, courseId)).thenReturn(true);
        when(coursePort.findLearningOutcomes(courseId)).thenReturn(targets);
        when(historyPort.load(studentId, courseId)).thenReturn(List.of());

        var result = service.predictAll(new PredictLearnerModelCommand(
                courseId, null, LearnerModelType.DKT_CUSTOM));

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
        when(actorPort.requireStudentId()).thenReturn(studentId);
        when(coursePort.isStudentEnrolled(studentId, courseId)).thenReturn(true);
        when(coursePort.findLearningOutcome(courseId, target.id())).thenReturn(Optional.of(target));
        when(historyPort.load(studentId, courseId)).thenReturn(List.of(history));
        when(inferencePort.predict(
                LearnerModelType.AKT_CUSTOM, studentId, List.of(history), List.of(target)))
                .thenReturn(List.of(new ModelOutcomePrediction(
                        5, 0.75, PredictionStatus.PREDICTED, List.of(9L))));

        var result = service.predictSingle(new PredictLearnerModelCommand(
                courseId, target.id(), LearnerModelType.AKT_CUSTOM));

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
        when(actorPort.requireStudentId()).thenReturn(studentId);
        when(coursePort.isStudentEnrolled(studentId, courseId)).thenReturn(true);
        when(coursePort.findLearningOutcomes(courseId)).thenReturn(List.of(target));
        when(historyPort.load(studentId, courseId)).thenReturn(List.of(history));

        assertThatThrownBy(() -> service.predictAll(new PredictLearnerModelCommand(
                courseId, null, LearnerModelType.AKT_PYKT)))
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
    void rejectsAStudentWhoIsNotEnrolledBeforeLoadingHistory() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(actorPort.requireStudentId()).thenReturn(studentId);
        when(coursePort.isStudentEnrolled(studentId, courseId)).thenReturn(false);

        assertThatThrownBy(() -> service.predictAll(new PredictLearnerModelCommand(
                courseId, null, LearnerModelType.DKT_CUSTOM)))
                .isInstanceOf(LearnerModelAccessDeniedException.class);

        verify(historyPort, never()).load(studentId, courseId);
    }

    @Test
    void returnsNotFoundWhenRequestedOutcomeIsOutsideTheCourse() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID outcomeId = UUID.randomUUID();
        when(actorPort.requireStudentId()).thenReturn(studentId);
        when(coursePort.isStudentEnrolled(studentId, courseId)).thenReturn(true);
        when(coursePort.findLearningOutcome(courseId, outcomeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.predictSingle(new PredictLearnerModelCommand(
                courseId, outcomeId, LearnerModelType.AKT_CUSTOM)))
                .isInstanceOf(LearnerModelNotFoundException.class);

        verify(historyPort, never()).load(studentId, courseId);
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
