package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.attempt.adapter.in.response.GetAttemptQuizSessionResponse;
import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.CurrentActor;
import edu.ai.lms.core.attempt.app.domain.QuizAttemptPolicy;
import edu.ai.lms.core.attempt.app.exception.AttemptNotFoundException;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.attempt.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuizResponse;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetQuizUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class GetAttemptQuizSessionServiceUT {

    @Mock private LoadSignedInUserPort loadSignedInUserPort;
    @Mock private LoadQuizAttemptPolicyPort loadQuizAttemptPolicyPort;
    @Mock private LoadAttemptPort loadAttemptPort;
    @Mock private AttemptAutoCloseService attemptAutoCloseService;
    @Mock private GetQuizUseCase getQuizUseCase;
    @InjectMocks private GetAttemptQuizSessionService service;

    @Test
    void returnsOnlyStudentSafeQuestionsForStartedAttempt() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        LocalDateTime startedAt = LocalDateTime.now().minusMinutes(2);

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(CurrentActor.builder().id(studentId).role(Roles.STUDENT).build()));
        when(loadQuizAttemptPolicyPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)).thenReturn(Optional.of(QuizAttemptPolicy.builder().quizId(quizId).courseId(courseId).unlimited(true).build()));
        when(loadAttemptPort.getLatestStartedAttempt(studentId, quizId)).thenReturn(Optional.of(Attempt.builder().id(attemptId).quizId(quizId).studentId(studentId).startTime(startedAt).status(Attempt.AttemptStatus.STARTED).build()));
        when(getQuizUseCase.getQuiz(courseId, quizId)).thenReturn(GetQuizResponse.builder()
                .quizId(quizId).quizTitle("Quiz3 base")
                .questions(List.of(GetQuizResponse.QuestionSummary.builder().questionId(questionId).content("Question").contentFormat(RichContentFormat.TIPTAP_HTML).type("MULTIPLE_CHOICE")
                        .answers(List.of(GetQuizResponse.AnswerSummary.builder().answerNumber(7L).content("Answer").contentFormat(RichContentFormat.TIPTAP_HTML).correct(true).explanation("Hidden").build()))
                        .build()))
                .build());

        GetAttemptQuizSessionResponse response = service.getSession(courseId, quizId);

        assertEquals(attemptId, response.getAttemptId());
        assertEquals(questionId, response.getQuestions().getFirst().getQuestionId());
        assertEquals(7L, response.getQuestions().getFirst().getAnswers().getFirst().getAnswerNumber());
        assertEquals("Answer", response.getQuestions().getFirst().getAnswers().getFirst().getContent());
        assertNull(response.getDueTime());
        verify(attemptAutoCloseService).isAlreadyStartedAttemptExpiredValidation(eq(studentId), eq(courseId), eq(quizId), any(LocalDateTime.class));
    }

    @Test
    void rejectsSessionWithoutStartedAttempt() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(CurrentActor.builder().id(studentId).role(Roles.STUDENT).build()));
        when(loadQuizAttemptPolicyPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)).thenReturn(Optional.of(QuizAttemptPolicy.builder().quizId(quizId).courseId(courseId).unlimited(true).build()));
        when(loadAttemptPort.getLatestStartedAttempt(studentId, quizId)).thenReturn(Optional.empty());

        assertThrows(AttemptNotFoundException.class, () -> service.getSession(courseId, quizId));
    }
}
