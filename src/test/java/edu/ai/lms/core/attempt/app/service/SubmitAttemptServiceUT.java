package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.AttemptQuestionResult;
import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.domain.QuizAttemptPolicy;
import edu.ai.lms.core.attempt.app.domain.SubmittedAnswer;
import edu.ai.lms.core.attempt.app.port.in.command.SubmitAttemptCommand;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.AttemptCodeSubmissionSnapshotPort;
import edu.ai.lms.core.attempt.app.port.out.AttemptQuestionResultPort;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private AttemptCodeSubmissionSnapshotPort codeSubmissionSnapshotPort;
    @Mock
    private AttemptQuestionResultPort attemptQuestionResultPort;
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
        UUID trueFalseQuestionId = UUID.randomUUID();

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
                .submittedAnswers(List.of(
                        SubmittedAnswer.builder().questionId(questionId).studentAnswerId(2L).build(),
                        SubmittedAnswer.builder().questionId(trueFalseQuestionId).studentAnswerId(1L).build()))
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(user));
        when(loadQuizAttemptPolicyPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)).thenReturn(Optional.of(policy));
        when(loadAttemptPort.getLatestStartedAttempt(studentId, quizId)).thenReturn(Optional.of(startedAttempt));
        when(loadQuizAttemptPolicyPort.getQuestionAnswerKeys(quizId)).thenReturn(List.of(
                QuestionAnswerKey.builder()
                        .questionId(questionId)
                        .ktQuestionId(1L)
                        .questionType(QuestionAnswerKey.QuestionType.MULTIPLE_CHOICE)
                        .correctAnswerNumber(2L)
                        .learningOutcomes(List.of(new QuestionAnswerKey.LearningOutcomeSnapshot(
                                UUID.randomUUID(), 1L)))
                        .build(),
                QuestionAnswerKey.builder()
                        .questionId(trueFalseQuestionId)
                        .ktQuestionId(2L)
                        .questionType(QuestionAnswerKey.QuestionType.TRUE_FALSE)
                        .correctAnswerNumber(2L)
                        .learningOutcomes(List.of(new QuestionAnswerKey.LearningOutcomeSnapshot(
                                UUID.randomUUID(), 2L)))
                        .build()));
        when(attemptScoringService.calculateScore(any(), any(), any(), any()))
            .thenReturn(new AttemptScoringService.ScoringResult(10.0, 0.0, false));
        when(saveAttemptPort.save(any(Attempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = submitAttemptService.submitAttempt(command);

        assertEquals("FINISHED", response.getStatus());
        assertEquals(10.0, response.getScore());

        ArgumentCaptor<Attempt> captor = ArgumentCaptor.forClass(Attempt.class);
        verify(saveAttemptPort).save(captor.capture());
        assertEquals(Attempt.AttemptStatus.FINISHED, captor.getValue().getStatus());
        assertEquals(10.0, captor.getValue().getScore());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AttemptQuestionResult>> resultCaptor = ArgumentCaptor.forClass(List.class);
        verify(attemptQuestionResultPort).upsertAll(resultCaptor.capture());
        assertEquals(2, resultCaptor.getValue().size());
        AttemptQuestionResult result = resultCaptor.getValue().getFirst();
        assertEquals(questionId, result.getQuestionId());
        assertEquals(1L, result.getKtQuestionId());
        assertEquals(0, result.getInteractionOrder());
        assertTrue(result.getCorrect());
        assertEquals(1, result.getLearningOutcomeMappingCount());
        AttemptQuestionResult trueFalseResult = resultCaptor.getValue().get(1);
        assertEquals(trueFalseQuestionId, trueFalseResult.getQuestionId());
        assertEquals(1, trueFalseResult.getInteractionOrder());
        assertFalse(trueFalseResult.getCorrect());
        verify(unscheduleAttemptAutoClosePort).unschedule(eq(startedAttempt.getId()));
    }

    @Test
    void snapshotsPartialAndPendingCodeRunnerResults() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID partialQuestionId = UUID.randomUUID();
        UUID pendingQuestionId = UUID.randomUUID();
        UUID partialSubmissionId = UUID.randomUUID();
        UUID pendingSubmissionId = UUID.randomUUID();
        Attempt attempt = Attempt.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .quizId(quizId)
                .status(Attempt.AttemptStatus.STARTED)
                .dueTime(LocalDateTime.now().plusMinutes(10))
                .build();
        List<QuestionAnswerKey> keys = List.of(
                QuestionAnswerKey.builder()
                        .questionId(partialQuestionId).ktQuestionId(10L)
                        .questionType(QuestionAnswerKey.QuestionType.CODE_RUNNER)
                        .learningOutcomes(List.of()).build(),
                QuestionAnswerKey.builder()
                        .questionId(pendingQuestionId).ktQuestionId(11L)
                        .questionType(QuestionAnswerKey.QuestionType.CODE_RUNNER)
                        .learningOutcomes(List.of()).build());
        List<SubmittedAnswer> answers = List.of(
                SubmittedAnswer.builder().questionId(partialQuestionId)
                        .codeSubmissionId(partialSubmissionId).build(),
                SubmittedAnswer.builder().questionId(pendingQuestionId)
                        .codeSubmissionId(pendingSubmissionId).build());

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(
                CurrentActor.builder().id(studentId).role(Roles.STUDENT).build()));
        when(loadQuizAttemptPolicyPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)).thenReturn(Optional.of(
                QuizAttemptPolicy.builder().quizId(quizId).courseId(courseId).timeLimitMinutes(30).build()));
        when(loadAttemptPort.getLatestStartedAttempt(studentId, quizId)).thenReturn(Optional.of(attempt));
        when(loadQuizAttemptPolicyPort.getQuestionAnswerKeys(quizId)).thenReturn(keys);
        when(attemptScoringService.calculateScore(keys, answers, attempt.getId(), studentId))
                .thenReturn(new AttemptScoringService.ScoringResult(0.0, 0.0, true));
        when(attemptScoringService.findCompletedCodeRunnerRatio(
                partialSubmissionId, attempt.getId(), studentId, partialQuestionId))
                .thenReturn(java.util.OptionalDouble.of(0.5));
        when(attemptScoringService.findCompletedCodeRunnerRatio(
                pendingSubmissionId, attempt.getId(), studentId, pendingQuestionId))
                .thenReturn(java.util.OptionalDouble.empty());
        when(saveAttemptPort.save(any(Attempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        submitAttemptService.submitAttempt(SubmitAttemptCommand.builder()
                .courseId(courseId).quizId(quizId).timeSpentSeconds(60)
                .submittedAnswers(answers).build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AttemptQuestionResult>> resultCaptor = ArgumentCaptor.forClass(List.class);
        verify(attemptQuestionResultPort).upsertAll(resultCaptor.capture());
        assertEquals(2, resultCaptor.getValue().size());
        assertFalse(resultCaptor.getValue().get(0).getCorrect());
        assertNull(resultCaptor.getValue().get(1).getCorrect());
        assertEquals(Attempt.AttemptStatus.FINALIZING, attempt.getStatus());
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
