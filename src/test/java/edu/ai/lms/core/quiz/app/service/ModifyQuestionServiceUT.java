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
import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyQuestionResponse;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuestionNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuestionCommand;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;

@ExtendWith(MockitoExtension.class)
class ModifyQuestionServiceUT {

    @Mock
    private SaveQuestionPort saveQuestionPort;

    @Mock
    private LoadQuestionPort loadQuestionPort;

    @Mock
    private LoadQuizPort loadQuizPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private ModifyQuestionService modifyQuestionService;

    private User mockTeacher;
    private UUID teacherId;
    private UUID quizId;
    private UUID questionId;
    private Quiz existingQuiz;
    private Question existingQuestion;
    private ModifyQuestionCommand validCommand;

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
                .content("Original Question")
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .points(10.0)
                .quizId(quizId)
                .createdAt(LocalDateTime.now())
                .build();

        Question updatedQuestion = Question.builder()
                .content("Updated Question")
                .type(Question.QuestionType.TRUE_FALSE)
                .points(20.0)
                .build();

        validCommand = ModifyQuestionCommand.builder()
                .quizId(quizId)
                .questionId(questionId)
                .question(updatedQuestion)
                .build();
    }

    @Test
    @DisplayName("Should successfully modify a question with valid details")
    void testModifyQuestion_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadQuestionPort.findByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));

        Question savedQuestion = Question.builder()
                .id(questionId)
                .content("Updated Question")
                .type(Question.QuestionType.TRUE_FALSE)
                .points(20.0)
                .quizId(quizId)
                .updatedAt(LocalDateTime.now())
                .build();
        when(saveQuestionPort.save(any(Question.class))).thenReturn(savedQuestion);

        // Act
        ModifyQuestionResponse response = modifyQuestionService.modifyQuestion(validCommand);

        // Assert
        assertNotNull(response);
        assertEquals(questionId, response.getQuestionId());
        assertEquals("Updated Question", response.getContent());
        assertEquals("TRUE_FALSE", response.getType());
        assertEquals(20.0, response.getPoints());
        assertEquals(quizId, response.getQuizId());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadQuizPort, times(1)).findById(quizId);
        verify(loadQuestionPort, times(1)).findByIdWithoutAnswers(questionId);
        verify(saveQuestionPort, times(1)).save(any(Question.class));
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testModifyQuestion_WithUnauthenticatedUser_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> modifyQuestionService.modifyQuestion(validCommand));

        assertEquals("User not authenticated", exception.getMessage());

        verify(saveQuestionPort, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("Should throw QuizNotFoundException when quiz does not exist")
    void testModifyQuestion_WithNonExistentQuiz_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.empty());

        // Act & Assert
        QuizNotFoundException exception = assertThrows(QuizNotFoundException.class,
                () -> modifyQuestionService.modifyQuestion(validCommand));

        assertEquals("Quiz not found with id: " + quizId, exception.getMessage());

        verify(saveQuestionPort, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not the quiz owner")
    void testModifyQuestion_WithNonOwner_ShouldThrowException() {
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
                () -> modifyQuestionService.modifyQuestion(validCommand));

        assertEquals("User is not authorized to modify questions in this quiz", exception.getMessage());

        verify(saveQuestionPort, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("Should throw QuestionNotFoundException when question does not exist")
    void testModifyQuestion_WithNonExistentQuestion_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadQuestionPort.findByIdWithoutAnswers(questionId)).thenReturn(Optional.empty());

        // Act & Assert
        QuestionNotFoundException exception = assertThrows(QuestionNotFoundException.class,
                () -> modifyQuestionService.modifyQuestion(validCommand));

        assertEquals("Question not found with id: " + questionId, exception.getMessage());

        verify(saveQuestionPort, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when question does not belong to the quiz")
    void testModifyQuestion_WithQuizMismatch_ShouldThrowException() {
        // Arrange
        UUID differentQuizId = UUID.randomUUID();
        existingQuestion.setQuizId(differentQuizId);

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadQuestionPort.findByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> modifyQuestionService.modifyQuestion(validCommand));

        assertEquals("Question does not belong to this quiz", exception.getMessage());

        verify(saveQuestionPort, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("Should preserve existing question id and quizId when modifying")
    void testModifyQuestion_ShouldPreserveExistingProperties() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadQuestionPort.findByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));

        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        Question savedQuestion = Question.builder()
                .id(questionId)
                .content("Updated Question")
                .quizId(quizId)
                .updatedAt(LocalDateTime.now())
                .build();
        when(saveQuestionPort.save(any(Question.class))).thenReturn(savedQuestion);

        // Act
        modifyQuestionService.modifyQuestion(validCommand);

        // Assert
        verify(saveQuestionPort).save(questionCaptor.capture());
        Question capturedQuestion = questionCaptor.getValue();
        assertEquals(questionId, capturedQuestion.getId());
        assertEquals(quizId, capturedQuestion.getQuizId());
    }
}
