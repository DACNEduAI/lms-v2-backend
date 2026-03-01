package edu.ai.lms.core.quiz.app.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.User;
import edu.ai.lms.core.quiz.adapter.in.http.response.DeleteQuestionResponse;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuestionNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.out.DeleteQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;

@ExtendWith(MockitoExtension.class)
class DeleteQuestionServiceUT {

    @Mock
    private DeleteQuestionPort deleteQuestionPort;

    @Mock
    private LoadQuestionPort loadQuestionPort;

    @Mock
    private LoadQuizPort loadQuizPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private DeleteQuestionService deleteQuestionService;

    private User mockTeacher;
    private UUID teacherId;
    private UUID quizId;
    private UUID questionId;
    private Quiz existingQuiz;
    private Question existingQuestion;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        quizId = UUID.randomUUID();
        questionId = UUID.randomUUID();

        mockTeacher = User.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        existingQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Midterm Quiz")
                .courseId(UUID.randomUUID())
                .teacherId(teacherId)
                .active(true)
                .build();

        existingQuestion = Question.builder()
                .id(questionId)
                .content("What is Java?")
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .points(10.0)
                .quizId(quizId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully delete a question with valid details")
    void testDeleteQuestion_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadQuestionPort.findByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));

        // Act
        DeleteQuestionResponse response = deleteQuestionService.deleteQuestion(quizId, questionId);

        // Assert
        assertNotNull(response);
        assertEquals(questionId, response.getQuestionId());
        assertEquals("Question deleted successfully", response.getMessage());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadQuizPort, times(1)).findById(quizId);
        verify(loadQuestionPort, times(1)).findByIdWithoutAnswers(questionId);
        verify(deleteQuestionPort, times(1)).deleteById(questionId);
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testDeleteQuestion_WithUnauthenticatedUser_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> deleteQuestionService.deleteQuestion(quizId, questionId));

        assertEquals("User not authenticated", exception.getMessage());

        verify(deleteQuestionPort, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw QuizNotFoundException when quiz does not exist")
    void testDeleteQuestion_WithNonExistentQuiz_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.empty());

        // Act & Assert
        QuizNotFoundException exception = assertThrows(QuizNotFoundException.class,
                () -> deleteQuestionService.deleteQuestion(quizId, questionId));

        assertEquals("Quiz not found with id: " + quizId, exception.getMessage());

        verify(deleteQuestionPort, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not the quiz owner")
    void testDeleteQuestion_WithNonOwner_ShouldThrowException() {
        // Arrange
        UUID anotherTeacherId = UUID.randomUUID();
        User anotherTeacher = User.builder()
                .id(anotherTeacherId)
                .role(Roles.TEACHER)
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(anotherTeacher));
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.of(existingQuiz));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> deleteQuestionService.deleteQuestion(quizId, questionId));

        assertEquals("User is not authorized to delete questions in this quiz", exception.getMessage());

        verify(deleteQuestionPort, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw QuestionNotFoundException when question does not exist")
    void testDeleteQuestion_WithNonExistentQuestion_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadQuestionPort.findByIdWithoutAnswers(questionId)).thenReturn(Optional.empty());

        // Act & Assert
        QuestionNotFoundException exception = assertThrows(QuestionNotFoundException.class,
                () -> deleteQuestionService.deleteQuestion(quizId, questionId));

        assertEquals("Question not found with id: " + questionId, exception.getMessage());

        verify(deleteQuestionPort, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when question does not belong to the quiz")
    void testDeleteQuestion_WithQuizMismatch_ShouldThrowException() {
        // Arrange
        UUID differentQuizId = UUID.randomUUID();
        existingQuestion.setQuizId(differentQuizId);

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadQuestionPort.findByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> deleteQuestionService.deleteQuestion(quizId, questionId));

        assertEquals("Question does not belong to this quiz", exception.getMessage());

        verify(deleteQuestionPort, never()).deleteById(any(UUID.class));
    }
}
