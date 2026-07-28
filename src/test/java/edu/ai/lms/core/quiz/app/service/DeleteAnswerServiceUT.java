package edu.ai.lms.core.quiz.app.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.ai.lms.core.quiz.app.service.crudquiz.DeleteAnswerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.adapter.in.http.response.DeleteAnswerResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.AnswerNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuestionNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.out.DeleteAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.LoadAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;

@ExtendWith(MockitoExtension.class)
class DeleteAnswerServiceUT {

    @Mock
    private DeleteAnswerPort deleteAnswerPort;

    @Mock
    private LoadAnswerPort loadAnswerPort;

    @Mock
    private LoadQuestionPort loadQuestionPort;

    @Mock
    private LoadQuizPort loadQuizPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private DeleteAnswerService deleteAnswerService;

    private CurrentActor mockTeacher;
    private UUID teacherId;
    private UUID quizId;
    private UUID questionId;
    private Long answerNumber;
    private Quiz existingQuiz;
    private Question existingQuestion;
    private Answer existingAnswer;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        quizId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        answerNumber = 1L;

        mockTeacher = CurrentActor.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        existingQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Midterm Quiz")
                .courseId(UUID.randomUUID())
                .teacherId(teacherId)
                .display(true)
                .build();

        existingQuestion = Question.builder()
                .id(questionId)
                .content("What is Java?")
                .quizId(quizId)
                .build();

        existingAnswer = Answer.builder()
                .answerNumber(answerNumber)
                .content("A programming language")
                .isCorrect(true)
                .questionId(questionId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully delete an answer with valid details")
    void testDeleteAnswer_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadAnswerPort.getByKey(questionId, answerNumber)).thenReturn(Optional.of(existingAnswer));

        // Act
        DeleteAnswerResponse response = deleteAnswerService.deleteAnswer(questionId, answerNumber);

        // Assert
        assertNotNull(response);
        assertEquals(answerNumber, response.getAnswerNumber());
        assertEquals(questionId, response.getQuestionId());
        assertEquals("Answer deleted successfully", response.getMessage());

        verify(loadSignedInUserPort, times(1)).getAuthenticatedUser();
        verify(loadQuestionPort, times(1)).getByIdWithoutAnswers(questionId);
        verify(loadQuizPort, times(1)).getById(quizId);
        verify(loadAnswerPort, times(1)).getByKey(questionId, answerNumber);
        verify(deleteAnswerPort, times(1)).deleteByKey(questionId, answerNumber);
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testDeleteAnswer_WithUnauthenticatedUser_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> deleteAnswerService.deleteAnswer(questionId, answerNumber));

        assertEquals("User not authenticated", exception.getMessage());

        verify(deleteAnswerPort, never()).deleteByKey(questionId, answerNumber);
    }

    @Test
    @DisplayName("Should throw QuestionNotFoundException when question does not exist")
    void testDeleteAnswer_WithNonExistentQuestion_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.empty());

        // Act & Assert
        QuestionNotFoundException exception = assertThrows(QuestionNotFoundException.class,
                () -> deleteAnswerService.deleteAnswer(questionId, answerNumber));

        assertEquals("Question not found with id: " + questionId, exception.getMessage());

        verify(deleteAnswerPort, never()).deleteByKey(questionId, answerNumber);
    }

    @Test
    @DisplayName("Should throw QuizNotFoundException when quiz does not exist")
    void testDeleteAnswer_WithNonExistentQuiz_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.empty());

        // Act & Assert
        QuizNotFoundException exception = assertThrows(QuizNotFoundException.class,
                () -> deleteAnswerService.deleteAnswer(questionId, answerNumber));

        assertEquals("Quiz not found", exception.getMessage());

        verify(deleteAnswerPort, never()).deleteByKey(questionId, answerNumber);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not the quiz owner")
    void testDeleteAnswer_WithNonOwner_ShouldThrowException() {
        // Arrange
        UUID anotherTeacherId = UUID.randomUUID();
        CurrentActor anotherTeacher = CurrentActor.builder()
                .id(anotherTeacherId)
                .role(Roles.TEACHER)
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(anotherTeacher));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> deleteAnswerService.deleteAnswer(questionId, answerNumber));

        assertEquals("User is not authorized to delete answers in this quiz", exception.getMessage());

        verify(deleteAnswerPort, never()).deleteByKey(questionId, answerNumber);
    }

    @Test
    @DisplayName("Should throw AnswerNotFoundException when answer does not exist")
    void testDeleteAnswer_WithNonExistentAnswer_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadAnswerPort.getByKey(questionId, answerNumber)).thenReturn(Optional.empty());

        // Act & Assert
        AnswerNotFoundException exception = assertThrows(AnswerNotFoundException.class,
                () -> deleteAnswerService.deleteAnswer(questionId, answerNumber));

        assertEquals("Answer not found", exception.getMessage());

        verify(deleteAnswerPort, never()).deleteByKey(questionId, answerNumber);
    }
}
