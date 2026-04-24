package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.QuizAttemptPolicy;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.attempt.app.port.out.SaveAttemptPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptAutoCloseServiceUT {

    @Mock
    private LoadAttemptPort loadAttemptPort;
    @Mock
    private LoadQuizAttemptPolicyPort loadQuizAttemptPolicyPort;
    @Mock
    private SaveAttemptPort saveAttemptPort;

    @InjectMocks
    private AttemptAutoCloseService attemptAutoCloseService;

    @Test
    @DisplayName("Should auto-finish timed started attempt with zero score when expired")
    void shouldAutoFinishTimedStartedAttemptWithZeroScoreWhenExpired() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Attempt startedAttempt = Attempt.builder()
                .id(attemptId)
                .studentId(studentId)
                .quizId(quizId)
                .status(Attempt.AttemptStatus.STARTED)
                .dueTime(now.minusSeconds(6))
                .build();

        QuizAttemptPolicy timedPolicy = QuizAttemptPolicy.builder()
                .quizId(quizId)
                .courseId(courseId)
                .unlimited(false)
                .timeLimitMinutes(2)
                .build();

        when(loadAttemptPort.getLatestStartedAttempt(studentId, quizId)).thenReturn(Optional.of(startedAttempt));
        when(loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)).thenReturn(Optional.of(timedPolicy));

        attemptAutoCloseService.isAlreadyStartedAttemptExpiredValidation(studentId, courseId, quizId, now);

        verify(saveAttemptPort).closeIfStarted(
                eq(attemptId),
                eq(Attempt.AttemptStatus.FINISHED),
                eq(0.0),
                eq("Attempt expired before submission"),
                eq(now)
        );
    }

    @Test
    @DisplayName("Should auto-finish untimed started attempt with zero score when expired")
    void shouldAutoFinishUntimedStartedAttemptWithZeroScoreWhenExpired() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Attempt startedAttempt = Attempt.builder()
                .id(attemptId)
                .studentId(studentId)
                .quizId(quizId)
                .status(Attempt.AttemptStatus.STARTED)
                .dueTime(now.minusSeconds(6))
                .build();

        QuizAttemptPolicy untimedPolicy = QuizAttemptPolicy.builder()
                .quizId(quizId)
                .courseId(courseId)
                .unlimited(false)
                .timeLimitMinutes(null)
                .build();

        when(loadAttemptPort.getLatestStartedAttempt(studentId, quizId)).thenReturn(Optional.of(startedAttempt));
        when(loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)).thenReturn(Optional.of(untimedPolicy));

        attemptAutoCloseService.isAlreadyStartedAttemptExpiredValidation(studentId, courseId, quizId, now);

        verify(saveAttemptPort).closeIfStarted(
                eq(attemptId),
                eq(Attempt.AttemptStatus.FINISHED),
                eq(0.0),
                eq("Attempt was not submitted before due time"),
                eq(now)
        );
    }

    @Test
    @DisplayName("Should auto-finish timed attempt through Quartz entrypoint when expired")
    void shouldAutoFinishTimedAttemptThroughQuartzEntrypointWhenExpired() {
        UUID attemptId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Attempt startedAttempt = Attempt.builder()
                .id(attemptId)
                .quizId(quizId)
                .status(Attempt.AttemptStatus.STARTED)
                .dueTime(now.minusSeconds(6))
                .build();

        QuizAttemptPolicy timedPolicy = QuizAttemptPolicy.builder()
                .quizId(quizId)
                .unlimited(false)
                .timeLimitMinutes(2)
                .build();

        when(loadAttemptPort.getById(attemptId)).thenReturn(Optional.of(startedAttempt));
        when(loadQuizAttemptPolicyPort.getQuizAttemptPolicy(null, quizId)).thenReturn(Optional.of(timedPolicy));

        attemptAutoCloseService.closeAttemptIfStillStarted(attemptId, now);

        verify(saveAttemptPort).closeIfStarted(
                eq(attemptId),
                eq(Attempt.AttemptStatus.FINISHED),
                eq(0.0),
                eq("Attempt expired before submission"),
                eq(now)
        );
    }
}
