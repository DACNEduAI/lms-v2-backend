package edu.ai.lms.core.quiz.app.service.quizworkflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.domain.QuizLifecycleStatus;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankAnswer;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankQuestion;
import edu.ai.lms.core.quiz.app.exception.QuizLifecycleConflictException;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionBankItemPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.ManageQuizLifecyclePort;
import edu.ai.lms.core.quiz.app.port.out.ManageQuizReleasePort;
import edu.ai.lms.core.quiz.app.port.out.SaveAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;

@ExtendWith(MockitoExtension.class)
class QuizWorkflowServiceUT {
    @Mock LoadSignedInUserPort signedInUserPort;
    @Mock LoadQuizPort loadQuizPort;
    @Mock ManageQuizLifecyclePort lifecyclePort;
    @Mock ManageQuizReleasePort releasePort;
    @Mock QuizAccessPolicy accessPolicy;
    @Mock LoadQuestionBankItemPort loadBankItemPort;
    @Mock LoadQuestionPort loadQuestionPort;
    @Mock SaveQuestionPort saveQuestionPort;
    @Mock SaveAnswerPort saveAnswerPort;
    @Mock SaveQuestionLearningOutcomePort saveQuestionLearningOutcomePort;
    @InjectMocks QuizWorkflowService service;

    @Test
    void copiesValidatedBankQuestionIntoIndependentQuizSnapshot() {
        UUID actorId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID bankQuestionId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        Quiz draft = Quiz.builder().id(quizId).courseId(courseId).teacherId(actorId)
                .version(2).lifecycleStatus(QuizLifecycleStatus.DRAFT).build();
        QuestionBankQuestion bankQuestion = QuestionBankQuestion.builder()
                .id(bankQuestionId).courseId(courseId).content("Validated question")
                .questionType(Question.QuestionType.MULTIPLE_CHOICE)
                .answers(List.of(
                        QuestionBankAnswer.builder().content("A").correct(true).build(),
                        QuestionBankAnswer.builder().content("B").correct(false).build()))
                .build();
        Question snapshot = Question.builder().id(snapshotId).quizId(quizId)
                .content("Validated question").type(Question.QuestionType.MULTIPLE_CHOICE)
                .sourceBankQuestionId(bankQuestionId).build();

        when(signedInUserPort.getAuthenticatedUser())
                .thenReturn(Optional.of(CurrentActor.builder().id(actorId).role(Roles.TEACHER).build()));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(draft));
        when(loadBankItemPort.findBankByCourseIdAndId(courseId, bankQuestionId))
                .thenReturn(Optional.of(bankQuestion));
        when(saveQuestionPort.save(any())).thenReturn(snapshot);
        when(loadQuestionPort.getById(snapshotId)).thenReturn(Optional.of(snapshot));
        when(lifecyclePort.touchContent(courseId, quizId, 2, QuizLifecycleStatus.DRAFT)).thenReturn(true);

        Question result = service.addBankQuestions(courseId, quizId, 2, List.of(bankQuestionId)).getFirst();

        assertEquals(bankQuestionId, result.getSourceBankQuestionId());
        assertEquals("Validated question", result.getContent());
        verify(saveAnswerPort, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void teacherPublishesDraftDirectlyAfterStructuralValidation() {
        UUID actorId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        Question question = Question.builder()
                .content("What is 2 + 2?")
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .answers(List.of(
                        Answer.builder().content("4").isCorrect(true).build(),
                        Answer.builder().content("5").isCorrect(false).build()))
                .build();
        Quiz draft = Quiz.builder().id(quizId).courseId(courseId).teacherId(actorId)
                .version(3).lifecycleStatus(QuizLifecycleStatus.DRAFT)
                .questions(List.of(question)).build();
        Quiz published = Quiz.builder().id(quizId).courseId(courseId).teacherId(actorId)
                .version(4).lifecycleStatus(QuizLifecycleStatus.PUBLISHED).display(true).build();

        when(signedInUserPort.getAuthenticatedUser())
                .thenReturn(Optional.of(CurrentActor.builder().id(actorId).role(Roles.TEACHER).build()));
        when(loadQuizPort.getByIdWithQuestionsAndAnswers(courseId, quizId)).thenReturn(Optional.of(draft));
        when(lifecyclePort.transition(eq(courseId), eq(quizId), eq(3L),
                eq(QuizLifecycleStatus.DRAFT), eq(QuizLifecycleStatus.PUBLISHED),
                eq(true), eq(actorId), any())).thenReturn(true);
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(published));

        Quiz result = service.publish(courseId, quizId, 3);

        assertEquals(QuizLifecycleStatus.PUBLISHED, result.getLifecycleStatus());
        verify(accessPolicy).requireCourseTeacher(any(), eq(draft));
        verify(releasePort).publishRelease(eq(quizId), eq(4L), eq(actorId), any());
    }

    @Test
    void publishRejectsInvalidObjectiveQuestion() {
        UUID actorId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        Quiz draft = Quiz.builder().id(quizId).courseId(courseId).teacherId(actorId)
                .lifecycleStatus(QuizLifecycleStatus.DRAFT)
                .questions(List.of(Question.builder()
                        .content("Invalid MCQ")
                        .type(Question.QuestionType.MULTIPLE_CHOICE)
                        .answers(List.of(Answer.builder().content("Only answer").isCorrect(true).build()))
                        .build()))
                .build();

        when(signedInUserPort.getAuthenticatedUser())
                .thenReturn(Optional.of(CurrentActor.builder().id(actorId).role(Roles.TEACHER).build()));
        when(loadQuizPort.getByIdWithQuestionsAndAnswers(courseId, quizId)).thenReturn(Optional.of(draft));

        assertThrows(QuizLifecycleConflictException.class,
                () -> service.publish(courseId, quizId, 0));
    }
}
