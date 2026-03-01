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
import org.mockito.ArgumentCaptor;
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
import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyQuizResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuizCommand;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizPort;

@ExtendWith(MockitoExtension.class)
class ModifyQuizServiceUT {

    @Mock
    private SaveQuizPort saveQuizPort;

    @Mock
    private LoadQuizPort loadQuizPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private ModifyQuizService modifyQuizService;

    private User mockTeacher;
    private UUID teacherId;
    private UUID courseId;
    private UUID quizId;
    private Quiz existingQuiz;
    private ModifyQuizCommand validCommand;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        quizId = UUID.randomUUID();

        mockTeacher = User.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        existingQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Original Title")
                .timeLimitMinutes(60)
                .passScore(70.0)
                .courseId(courseId)
                .teacherId(teacherId)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        Quiz updatedQuiz = Quiz.builder()
                .quizTitle("Updated Title")
                .timeLimitMinutes(90)
                .passScore(80.0)
                .active(true)
                .build();

        validCommand = ModifyQuizCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .quiz(updatedQuiz)
                .build();
    }

    @Test
    @DisplayName("Should successfully modify a quiz with valid details")
    void testModifyQuiz_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.of(existingQuiz));

        Quiz savedQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Updated Title")
                .timeLimitMinutes(90)
                .passScore(80.0)
                .courseId(courseId)
                .teacherId(teacherId)
                .active(true)
                .updatedAt(LocalDateTime.now())
                .build();
        when(saveQuizPort.save(any(Quiz.class))).thenReturn(savedQuiz);

        // Act
        ModifyQuizResponse response = modifyQuizService.modifyQuiz(validCommand);

        // Assert
        assertNotNull(response);
        assertEquals(quizId, response.getQuizId());
        assertEquals("Updated Title", response.getQuizTitle());
        assertEquals(90, response.getTimeLimitMinutes());
        assertEquals(80.0, response.getPassScore());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadQuizPort, times(1)).findById(quizId);
        verify(saveQuizPort, times(1)).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testModifyQuiz_WithUnauthenticatedUser_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> modifyQuizService.modifyQuiz(validCommand));

        assertEquals("User not authenticated", exception.getMessage());

        verify(saveQuizPort, never()).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Should throw QuizNotFoundException when quiz does not exist")
    void testModifyQuiz_WithNonExistentQuiz_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.empty());

        // Act & Assert
        QuizNotFoundException exception = assertThrows(QuizNotFoundException.class,
                () -> modifyQuizService.modifyQuiz(validCommand));

        assertEquals("Quiz not found with id: " + quizId, exception.getMessage());

        verify(saveQuizPort, never()).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when quiz does not belong to the course")
    void testModifyQuiz_WithCourseMismatch_ShouldThrowException() {
        // Arrange
        UUID differentCourseId = UUID.randomUUID();
        existingQuiz.setCourseId(differentCourseId);

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.of(existingQuiz));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> modifyQuizService.modifyQuiz(validCommand));

        assertEquals("Quiz does not belong to this course", exception.getMessage());

        verify(saveQuizPort, never()).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not the quiz owner")
    void testModifyQuiz_WithNonOwner_ShouldThrowException() {
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
                () -> modifyQuizService.modifyQuiz(validCommand));

        assertEquals("User is not authorized to modify this quiz", exception.getMessage());

        verify(saveQuizPort, never()).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Should preserve existing quiz properties when modifying")
    void testModifyQuiz_ShouldPreserveExistingProperties() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.of(existingQuiz));

        ArgumentCaptor<Quiz> quizCaptor = ArgumentCaptor.forClass(Quiz.class);
        Quiz savedQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Updated Title")
                .courseId(courseId)
                .teacherId(teacherId)
                .active(true)
                .updatedAt(LocalDateTime.now())
                .build();
        when(saveQuizPort.save(any(Quiz.class))).thenReturn(savedQuiz);

        // Act
        modifyQuizService.modifyQuiz(validCommand);

        // Assert
        verify(saveQuizPort).save(quizCaptor.capture());
        Quiz capturedQuiz = quizCaptor.getValue();
        assertEquals(quizId, capturedQuiz.getId());
        assertEquals(courseId, capturedQuiz.getCourseId());
        assertEquals(teacherId, capturedQuiz.getTeacherId());
    }
}
