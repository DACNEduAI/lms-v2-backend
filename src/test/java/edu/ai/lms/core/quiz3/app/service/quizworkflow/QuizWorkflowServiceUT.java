package edu.ai.lms.core.quiz3.app.service.quizworkflow;

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
import edu.ai.lms.core.quiz3.app.domain.Answer;
import edu.ai.lms.core.quiz3.app.domain.CurrentActor;
import edu.ai.lms.core.quiz3.app.domain.Question;
import edu.ai.lms.core.quiz3.app.domain.Quiz;
import edu.ai.lms.core.quiz3.app.domain.QuizLifecycleStatus;
import edu.ai.lms.core.quiz3.app.exception.QuizLifecycleConflictException;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz3.app.port.out.ManageQuizLifecyclePort;
import edu.ai.lms.core.quiz3.app.port.out.ManageQuizReleasePort;
import edu.ai.lms.core.quiz3.app.port.out.SaveAnswerPort;
import edu.ai.lms.core.quiz3.app.port.out.SaveQuestionLearningOutcomePort;
import edu.ai.lms.core.quiz3.app.port.out.SaveQuestionPort;

@ExtendWith(MockitoExtension.class)
class QuizWorkflowServiceUT {
    @Mock LoadSignedInUserPort signedInUserPort;
    @Mock LoadQuizPort loadQuizPort;
    @Mock ManageQuizLifecyclePort lifecyclePort;
    @Mock ManageQuizReleasePort releasePort;
    @Mock QuizAccessPolicy accessPolicy;
    @Mock LoadQuestionPort loadQuestionPort;
    @Mock SaveQuestionPort saveQuestionPort;
    @Mock SaveAnswerPort saveAnswerPort;
    @Mock SaveQuestionLearningOutcomePort saveQuestionLearningOutcomePort;
    @InjectMocks QuizWorkflowService service;

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
