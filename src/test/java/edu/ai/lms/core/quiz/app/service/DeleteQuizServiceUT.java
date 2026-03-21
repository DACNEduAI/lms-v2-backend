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
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.DeleteQuizResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.out.DeleteQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;

@ExtendWith(MockitoExtension.class)
class DeleteQuizServiceUT {

    @Mock
    private DeleteQuizPort deleteQuizPort;

    @Mock
    private LoadQuizPort loadQuizPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private DeleteQuizService deleteQuizService;

    private UserAuth mockTeacher;
    private UUID teacherId;
    private UUID courseId;
    private UUID quizId;
    private Quiz existingQuiz;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        quizId = UUID.randomUUID();

        mockTeacher = UserAuth.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        existingQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Quiz to Delete")
                .courseId(courseId)
                .teacherId(teacherId)
                .display(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully delete a quiz with valid details")
    void testDeleteQuiz_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

        // Act
        DeleteQuizResponse response = deleteQuizService.deleteQuiz(courseId, quizId);

        // Assert
        assertNotNull(response);
        assertEquals(quizId, response.getQuizId());
        assertEquals("Quiz deleted successfully", response.getMessage());

        verify(loadSignedInUserPort, times(1)).getAuthenticatedUser();
        verify(loadQuizPort, times(1)).getById(quizId);
        verify(deleteQuizPort, times(1)).deleteById(quizId);
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testDeleteQuiz_WithUnauthenticatedUser_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> deleteQuizService.deleteQuiz(courseId, quizId));

        assertEquals("User not authenticated", exception.getMessage());

        verify(deleteQuizPort, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw QuizNotFoundException when quiz does not exist")
    void testDeleteQuiz_WithNonExistentQuiz_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.empty());

        // Act & Assert
        QuizNotFoundException exception = assertThrows(QuizNotFoundException.class,
                () -> deleteQuizService.deleteQuiz(courseId, quizId));

        assertEquals("Quiz not found with id: " + quizId, exception.getMessage());

        verify(deleteQuizPort, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when quiz does not belong to the course")
    void testDeleteQuiz_WithCourseMismatch_ShouldThrowException() {
        // Arrange
        UUID differentCourseId = UUID.randomUUID();
        existingQuiz.setCourseId(differentCourseId);

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> deleteQuizService.deleteQuiz(courseId, quizId));

        assertEquals("Quiz does not belong to this course", exception.getMessage());

        verify(deleteQuizPort, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not the quiz owner")
    void testDeleteQuiz_WithNonOwner_ShouldThrowException() {
        // Arrange
        UUID anotherTeacherId = UUID.randomUUID();
        UserAuth anotherTeacher = UserAuth.builder()
                .id(anotherTeacherId)
                .role(Roles.TEACHER)
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(anotherTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> deleteQuizService.deleteQuiz(courseId, quizId));

        assertEquals("User is not authorized to delete this quiz", exception.getMessage());

        verify(deleteQuizPort, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("Should allow the student owner to delete their quiz")
    void testDeleteQuiz_AsStudentOwner_ShouldSucceed() {
        // Arrange
        UUID studentId = UUID.randomUUID();
        UserAuth mockStudent = UserAuth.builder()
                .id(studentId)
                .role(Roles.STUDENT)
                .build();

        existingQuiz.setTeacherId(null);
        existingQuiz.setStudentId(studentId);

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

        // Act
        DeleteQuizResponse response = deleteQuizService.deleteQuiz(courseId, quizId);

        // Assert
        assertNotNull(response);
        assertEquals(quizId, response.getQuizId());

        verify(deleteQuizPort, times(1)).deleteById(quizId);
    }
}
