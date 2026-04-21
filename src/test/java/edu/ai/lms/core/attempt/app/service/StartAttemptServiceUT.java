package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.QuizAttemptPolicy;
import edu.ai.lms.core.attempt.app.exception.AttemptLimitExceededException;
import edu.ai.lms.core.attempt.app.exception.InvalidAttemptSubmissionException;
import edu.ai.lms.core.attempt.app.port.in.command.StartAttemptCommand;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.attempt.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.attempt.app.port.out.ScheduleAttemptAutoClosePort;
import edu.ai.lms.core.attempt.app.port.out.SaveAttemptPort;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartAttemptServiceUT {

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;
    @Mock
    private LoadQuizAttemptPolicyPort loadQuizAttemptPolicyPort;
    @Mock
    private LoadAttemptPort loadAttemptPort;
    @Mock
    private SaveAttemptPort saveAttemptPort;
    @Mock
    private ScheduleAttemptAutoClosePort scheduleAttemptAutoClosePort;
    @Mock
    private AttemptAutoCloseService attemptAutoCloseService;

    @InjectMocks
    private StartAttemptService startAttemptService;

    @Test
    @DisplayName("Should start timed attempt successfully")
    void shouldStartTimedAttemptSuccessfully() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();

        UserAuth user = UserAuth.builder().id(studentId).role(Roles.STUDENT).build();
        QuizAttemptPolicy policy = QuizAttemptPolicy.builder()
                .quizId(quizId)
                .courseId(courseId)
            .unlimited(false)
                .timeLimitMinutes(30)
            .quizDueTime(LocalDateTime.now().plusHours(1))
                .attemptLimited(2)
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(user));
        when(loadQuizAttemptPolicyPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)).thenReturn(Optional.of(policy));
        when(loadAttemptPort.getLatestStartedAttempt(studentId, quizId)).thenReturn(Optional.empty());
        when(loadAttemptPort.countFinishedAttempts(studentId, quizId)).thenReturn(0L);
        when(saveAttemptPort.save(any(Attempt.class))).thenAnswer(invocation -> {
            Attempt attempt = invocation.getArgument(0);
            attempt.setId(UUID.randomUUID());
            return attempt;
        });

        var response = startAttemptService.startAttempt(StartAttemptCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .build());

        assertNotNull(response.getAttemptId());
        assertEquals("STARTED", response.getStatus());
        assertNotNull(response.getStartTime());
        assertNotNull(response.getDueTime());

        ArgumentCaptor<Attempt> captor = ArgumentCaptor.forClass(Attempt.class);
        verify(saveAttemptPort).save(captor.capture());
        assertEquals(Attempt.AttemptStatus.STARTED, captor.getValue().getStatus());
        verify(scheduleAttemptAutoClosePort).schedule(any(UUID.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should throw when attempt limit is reached")
    void shouldThrowWhenAttemptLimitReached() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();

        UserAuth user = UserAuth.builder().id(studentId).role(Roles.STUDENT).build();
        QuizAttemptPolicy policy = QuizAttemptPolicy.builder()
                .quizId(quizId)
                .courseId(courseId)
            .unlimited(false)
                .timeLimitMinutes(30)
            .quizDueTime(LocalDateTime.now().plusHours(1))
                .attemptLimited(1)
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(user));
        when(loadQuizAttemptPolicyPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)).thenReturn(Optional.of(policy));
        when(loadAttemptPort.getLatestStartedAttempt(studentId, quizId)).thenReturn(Optional.empty());
        when(loadAttemptPort.countFinishedAttempts(studentId, quizId)).thenReturn(1L);

        assertThrows(AttemptLimitExceededException.class, () -> startAttemptService.startAttempt(StartAttemptCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .build()));
    }

    @Test
    @DisplayName("Should use quiz due time for non-timed quiz")
    void shouldUseQuizDueTimeForNonTimedQuiz() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        LocalDateTime quizDueTime = LocalDateTime.now().plusHours(2);

        UserAuth user = UserAuth.builder().id(studentId).role(Roles.STUDENT).build();
        QuizAttemptPolicy policy = QuizAttemptPolicy.builder()
                .quizId(quizId)
                .courseId(courseId)
            .unlimited(false)
                .timeLimitMinutes(null)
                .quizDueTime(quizDueTime)
                .attemptLimited(3)
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(user));
        when(loadQuizAttemptPolicyPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)).thenReturn(Optional.of(policy));
        when(loadAttemptPort.getLatestStartedAttempt(studentId, quizId)).thenReturn(Optional.empty());
        when(loadAttemptPort.countFinishedAttempts(studentId, quizId)).thenReturn(0L);
        when(saveAttemptPort.save(any(Attempt.class))).thenAnswer(invocation -> {
            Attempt attempt = invocation.getArgument(0);
            attempt.setId(UUID.randomUUID());
            return attempt;
        });

        var response = startAttemptService.startAttempt(StartAttemptCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .build());

        assertEquals(quizDueTime, response.getDueTime());
    }

    @Test
    @DisplayName("Should allow unlimited attempt when non-timed quiz has no due time")
    void shouldAllowUnlimitedAttemptWhenNonTimedQuizHasNoDueTime() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();

        UserAuth user = UserAuth.builder().id(studentId).role(Roles.STUDENT).build();
        QuizAttemptPolicy policy = QuizAttemptPolicy.builder()
                .quizId(quizId)
                .courseId(courseId)
            .unlimited(true)
                .timeLimitMinutes(0)
                .quizDueTime(null)
                .attemptLimited(3)
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(user));
        when(loadQuizAttemptPolicyPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)).thenReturn(Optional.of(policy));
        when(loadAttemptPort.getLatestStartedAttempt(studentId, quizId)).thenReturn(Optional.empty());
        when(loadAttemptPort.countFinishedAttempts(studentId, quizId)).thenReturn(0L);
        when(saveAttemptPort.save(any(Attempt.class))).thenAnswer(invocation -> {
            Attempt attempt = invocation.getArgument(0);
            attempt.setId(UUID.randomUUID());
            return attempt;
        });

        var response = startAttemptService.startAttempt(StartAttemptCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .build());

        assertEquals("STARTED", response.getStatus());
        assertNull(response.getDueTime());
    }

    @Test
    @DisplayName("Should reject attempt before quiz start time when unlimited is false")
    void shouldRejectAttemptBeforeQuizStartTimeWhenLimited() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();

        UserAuth user = UserAuth.builder().id(studentId).role(Roles.STUDENT).build();
        QuizAttemptPolicy policy = QuizAttemptPolicy.builder()
                .quizId(quizId)
                .courseId(courseId)
                .unlimited(false)
                .quizStartTime(LocalDateTime.now().plusMinutes(5))
                .quizDueTime(LocalDateTime.now().plusHours(1))
                .attemptLimited(3)
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(user));
        when(loadQuizAttemptPolicyPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)).thenReturn(Optional.of(policy));
        when(loadAttemptPort.getLatestStartedAttempt(studentId, quizId)).thenReturn(Optional.empty());
        when(loadAttemptPort.countFinishedAttempts(studentId, quizId)).thenReturn(0L);

        InvalidAttemptSubmissionException exception = assertThrows(InvalidAttemptSubmissionException.class,
                () -> startAttemptService.startAttempt(StartAttemptCommand.builder()
                        .courseId(courseId)
                        .quizId(quizId)
                        .build()));

        assertEquals("Quiz has not started yet", exception.getMessage());
    }
}
