package edu.ai.lms.core.quiz.app.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyAnswerResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.AnswerNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuestionNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyAnswerCommand;
import edu.ai.lms.core.quiz.app.port.out.LoadAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveAnswerPort;

@ExtendWith(MockitoExtension.class)
class ModifyAnswerServiceUT {

    @Mock
    private SaveAnswerPort saveAnswerPort;

    @Mock
    private LoadAnswerPort loadAnswerPort;

    @Mock
    private LoadQuestionPort loadQuestionPort;

    @Mock
    private LoadQuizPort loadQuizPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private ModifyAnswerService modifyAnswerService;

    private UserAuth mockTeacher;
    private UUID teacherId;
    private UUID quizId;
    private UUID questionId;
    private Long answerNumber;
    private Quiz existingQuiz;
    private Question existingQuestion;
    private Answer existingAnswer;
    private ModifyAnswerCommand validCommand;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        quizId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        answerNumber = 1L;

        mockTeacher = UserAuth.builder()
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
                .content("Original Answer")
                .isCorrect(true)
                .explanation("Original explanation")
                .questionId(questionId)
                .createdAt(LocalDateTime.now())
                .build();

        Answer updatedAnswer = Answer.builder()
                .content("Updated Answer")
                .isCorrect(false)
                .explanation("Updated explanation")
                .build();

        validCommand = ModifyAnswerCommand.builder()
                .questionId(questionId)
                .answerNumber(answerNumber)
                .answer(updatedAnswer)
                .build();
    }

    @Test
    @DisplayName("Should successfully modify an answer with valid details")
    void testModifyAnswer_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadAnswerPort.getByKey(questionId, answerNumber)).thenReturn(Optional.of(existingAnswer));

        Answer savedAnswer = Answer.builder()
                .answerNumber(answerNumber)
                .content("Updated Answer")
                .isCorrect(false)
                .explanation("Updated explanation")
                .questionId(questionId)
                .updatedAt(LocalDateTime.now())
                .build();
        when(saveAnswerPort.save(any(Answer.class))).thenReturn(savedAnswer);

        // Act
        ModifyAnswerResponse response = modifyAnswerService.modifyAnswer(validCommand);

        // Assert
        assertNotNull(response);
        assertEquals(answerNumber, response.getAnswerNumber());
        assertEquals("Updated Answer", response.getContent());
        assertFalse(response.isCorrect());
        assertEquals("Updated explanation", response.getExplanation());
        assertEquals(questionId, response.getQuestionId());

        verify(loadSignedInUserPort, times(1)).getAuthenticatedUser();
        verify(loadQuestionPort, times(1)).getByIdWithoutAnswers(questionId);
        verify(loadQuizPort, times(1)).getById(quizId);
        verify(loadAnswerPort, times(1)).getByKey(questionId, answerNumber);
        verify(saveAnswerPort, times(1)).save(any(Answer.class));
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testModifyAnswer_WithUnauthenticatedUser_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> modifyAnswerService.modifyAnswer(validCommand));

        assertEquals("User not authenticated", exception.getMessage());

        verify(saveAnswerPort, never()).save(any(Answer.class));
    }

    @Test
    @DisplayName("Should throw QuestionNotFoundException when question does not exist")
    void testModifyAnswer_WithNonExistentQuestion_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.empty());

        // Act & Assert
        QuestionNotFoundException exception = assertThrows(QuestionNotFoundException.class,
                () -> modifyAnswerService.modifyAnswer(validCommand));

        assertEquals("Question not found with id: " + questionId, exception.getMessage());

        verify(saveAnswerPort, never()).save(any(Answer.class));
    }

    @Test
    @DisplayName("Should throw QuizNotFoundException when quiz does not exist")
    void testModifyAnswer_WithNonExistentQuiz_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.empty());

        // Act & Assert
        QuizNotFoundException exception = assertThrows(QuizNotFoundException.class,
                () -> modifyAnswerService.modifyAnswer(validCommand));

        assertEquals("Quiz not found", exception.getMessage());

        verify(saveAnswerPort, never()).save(any(Answer.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not the quiz owner")
    void testModifyAnswer_WithNonOwner_ShouldThrowException() {
        // Arrange
        UUID anotherTeacherId = UUID.randomUUID();
        UserAuth anotherTeacher = UserAuth.builder()
                .id(anotherTeacherId)
                .role(Roles.TEACHER)
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(anotherTeacher));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> modifyAnswerService.modifyAnswer(validCommand));

        assertEquals("User is not authorized to modify answers in this quiz", exception.getMessage());

        verify(saveAnswerPort, never()).save(any(Answer.class));
    }

    @Test
    @DisplayName("Should throw AnswerNotFoundException when answer does not exist")
    void testModifyAnswer_WithNonExistentAnswer_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadAnswerPort.getByKey(questionId, answerNumber)).thenReturn(Optional.empty());

        // Act & Assert
        AnswerNotFoundException exception = assertThrows(AnswerNotFoundException.class,
                () -> modifyAnswerService.modifyAnswer(validCommand));

        assertEquals("Answer not found", exception.getMessage());

        verify(saveAnswerPort, never()).save(any(Answer.class));
    }

    @Test
    @DisplayName("Should preserve existing answerNumber and questionId when modifying")
    void testModifyAnswer_ShouldPreserveExistingProperties() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadAnswerPort.getByKey(questionId, answerNumber)).thenReturn(Optional.of(existingAnswer));

        ArgumentCaptor<Answer> answerCaptor = ArgumentCaptor.forClass(Answer.class);
        Answer savedAnswer = Answer.builder()
                .answerNumber(answerNumber)
                .content("Updated Answer")
                .questionId(questionId)
                .updatedAt(LocalDateTime.now())
                .build();
        when(saveAnswerPort.save(any(Answer.class))).thenReturn(savedAnswer);

        // Act
        modifyAnswerService.modifyAnswer(validCommand);

        // Assert
        verify(saveAnswerPort).save(answerCaptor.capture());
        Answer capturedAnswer = answerCaptor.getValue();
        assertEquals(answerNumber, capturedAnswer.getAnswerNumber());
        assertEquals(questionId, capturedAnswer.getQuestionId());
    }
}
