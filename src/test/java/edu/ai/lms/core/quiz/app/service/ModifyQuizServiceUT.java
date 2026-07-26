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

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyQuizResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuizCommand;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizPort;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ModifyQuizServiceUT {

    @Mock
    private SaveQuizPort saveQuizPort;

    @Mock
    private LoadQuizPort loadQuizPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private LoadCourseMembershipPort loadCourseMembershipPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ModifyQuizService modifyQuizService;

    private CurrentActor mockTeacher;
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

        mockTeacher = CurrentActor.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        existingQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Original Title")
                .unlimited(false)
                .timeLimitMinutes(60)
                .passScore(70.0)
                .dueTime(LocalDateTime.now().plusHours(1))
                .courseId(courseId)
                .teacherId(teacherId)
                .display(true)
                .createdAt(LocalDateTime.now())
                .build();

        Quiz updatedQuiz = Quiz.builder()
                .quizTitle("Updated Title")
                .unlimited(false)
                .timeLimitMinutes(90)
                .passScore(80.0)
                .dueTime(LocalDateTime.now().plusHours(2))
                .display(true)
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
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

        Quiz savedQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Updated Title")
                .unlimited(false)
                .timeLimitMinutes(90)
                .passScore(80.0)
                .dueTime(validCommand.quiz().getDueTime())
                .courseId(courseId)
                .teacherId(teacherId)
                .display(true)
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

        verify(loadSignedInUserPort, times(1)).getAuthenticatedUser();
        verify(loadQuizPort, times(1)).getById(quizId);
        verify(saveQuizPort, times(1)).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testModifyQuiz_WithUnauthenticatedUser_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.empty());

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
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.empty());

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

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

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
        CurrentActor anotherTeacher = CurrentActor.builder()
                .id(anotherTeacherId)
                .role(Roles.TEACHER)
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(anotherTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

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
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

        ArgumentCaptor<Quiz> quizCaptor = ArgumentCaptor.forClass(Quiz.class);
        Quiz savedQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Updated Title")
                .unlimited(false)
                .courseId(courseId)
                .teacherId(teacherId)
                .dueTime(existingQuiz.getDueTime())
                .display(true)
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

        @Test
        @DisplayName("Should reject modify when unlimited is true but startTime or dueTime is provided")
        void testModifyQuiz_WithUnlimitedTrueAndProvidedTimes_ShouldThrow() {
                Quiz invalidUpdate = Quiz.builder()
                                .quizTitle("Invalid Update")
                                .unlimited(true)
                                .startTime(LocalDateTime.now().plusMinutes(1))
                                .dueTime(LocalDateTime.now().plusHours(1))
                                .build();

                ModifyQuizCommand invalidCommand = ModifyQuizCommand.builder()
                                .courseId(courseId)
                                .quizId(quizId)
                                .quiz(invalidUpdate)
                                .build();

                when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
                when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                () -> modifyQuizService.modifyQuiz(invalidCommand));

                assertEquals("When unlimited is true, startTime and dueTime must be null", exception.getMessage());
        }
}
