package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.domain.QuizAttemptPolicy;
import edu.ai.lms.core.attempt.app.domain.SubmittedAnswer;
import edu.ai.lms.core.attempt.app.port.in.command.SubmitAttemptCommand;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.attempt.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.attempt.app.port.out.SaveAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.UnscheduleAttemptAutoClosePort;
import edu.ai.lms.core.attempt.app.domain.CurrentActor;
import org.junit.jupiter.api.DisplayName;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitAttemptServiceUT {

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;
    @Mock
    private LoadQuizAttemptPolicyPort loadQuizAttemptPolicyPort;
    @Mock
    private LoadAttemptPort loadAttemptPort;
    @Mock
    private SaveAttemptPort saveAttemptPort;
    @Mock
    private UnscheduleAttemptAutoClosePort unscheduleAttemptAutoClosePort;
    @Mock
    private AttemptScoringService attemptScoringService;
    @Mock
    private AttemptAutoCloseService attemptAutoCloseService;

    @InjectMocks
    private SubmitAttemptService submitAttemptService;

    @Test
    @DisplayName("Should submit on time and calculate score")
    void shouldSubmitOnTime() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        CurrentActor user = CurrentActor.builder().id(studentId).role(Roles.STUDENT).build();
        QuizAttemptPolicy policy = QuizAttemptPolicy.builder().quizId(quizId).courseId(courseId).timeLimitMinutes(30).build();
        Attempt startedAttempt = Attempt.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .quizId(quizId)
                .status(Attempt.AttemptStatus.STARTED)
                .dueTime(LocalDateTime.now().plusMinutes(10))
                .build();

        SubmitAttemptCommand command = SubmitAttemptCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .timeSpentSeconds(120)
                .submittedAnswers(List.of(SubmittedAnswer.builder().questionId(questionId).studentAnswerId(2L).build()))
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(user));
        when(loadQuizAttemptPolicyPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)).thenReturn(Optional.of(policy));
        when(loadAttemptPort.getLatestStartedAttempt(studentId, quizId)).thenReturn(Optional.of(startedAttempt));
        when(loadQuizAttemptPolicyPort.getQuestionAnswerKeys(quizId)).thenReturn(List.of(QuestionAnswerKey.builder().build()));
        when(attemptScoringService.calculateScore(any(), any(), any(), any())).thenReturn(8.0);
        when(saveAttemptPort.save(any(Attempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = submitAttemptService.submitAttempt(command);

        assertEquals("FINISHED", response.getStatus());
        assertEquals(8.0, response.getScore());

        ArgumentCaptor<Attempt> captor = ArgumentCaptor.forClass(Attempt.class);
        verify(saveAttemptPort).save(captor.capture());
        assertEquals(Attempt.AttemptStatus.FINISHED, captor.getValue().getStatus());
        assertEquals(8.0, captor.getValue().getScore());
        verify(unscheduleAttemptAutoClosePort).unschedule(eq(startedAttempt.getId()));
    }

    @Test
    @DisplayName("Should mark timed attempt as error when submitted late")
    void shouldMarkTimedAttemptAsErrorWhenLate() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();

        CurrentActor user = CurrentActor.builder().id(studentId).role(Roles.STUDENT).build();
        QuizAttemptPolicy policy = QuizAttemptPolicy.builder().quizId(quizId).courseId(courseId).timeLimitMinutes(30).build();
        Attempt startedAttempt = Attempt.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .quizId(quizId)
                .status(Attempt.AttemptStatus.STARTED)
                .dueTime(LocalDateTime.now().minusMinutes(1))
                .build();

        SubmitAttemptCommand command = SubmitAttemptCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .timeSpentSeconds(180)
                .submittedAnswers(List.of())
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(user));
        when(loadQuizAttemptPolicyPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)).thenReturn(Optional.of(policy));
        when(loadAttemptPort.getLatestStartedAttempt(studentId, quizId)).thenReturn(Optional.of(startedAttempt));
        when(saveAttemptPort.save(any(Attempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = submitAttemptService.submitAttempt(command);

        assertEquals("ERROR", response.getStatus());
        assertNull(response.getScore());
        verify(unscheduleAttemptAutoClosePort).unschedule(eq(startedAttempt.getId()));
    }

    @Test
        @DisplayName("Should treat zero-minute late attempt as non-timed and finish with zero score")
    void shouldMarkNonTimedLateAttemptAsFinishedWithZero() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();

        CurrentActor user = CurrentActor.builder().id(studentId).role(Roles.STUDENT).build();
        QuizAttemptPolicy policy = QuizAttemptPolicy.builder().quizId(quizId).courseId(courseId).timeLimitMinutes(0).build();
        Attempt startedAttempt = Attempt.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .quizId(quizId)
                .status(Attempt.AttemptStatus.STARTED)
                .dueTime(LocalDateTime.now().minusMinutes(1))
                .build();

        SubmitAttemptCommand command = SubmitAttemptCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .timeSpentSeconds(250)
                .submittedAnswers(List.of())
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(user));
        when(loadQuizAttemptPolicyPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)).thenReturn(Optional.of(policy));
        when(loadAttemptPort.getLatestStartedAttempt(studentId, quizId)).thenReturn(Optional.of(startedAttempt));
        when(saveAttemptPort.save(any(Attempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = submitAttemptService.submitAttempt(command);

        assertEquals("FINISHED", response.getStatus());
        assertEquals(0.0, response.getScore());
        verify(unscheduleAttemptAutoClosePort).unschedule(eq(startedAttempt.getId()));

        verify(loadQuizAttemptPolicyPort).getQuizAttemptPolicy(eq(courseId), eq(quizId));
    }
}
