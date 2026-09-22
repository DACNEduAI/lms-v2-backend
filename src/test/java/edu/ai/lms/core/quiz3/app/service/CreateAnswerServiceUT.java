package edu.ai.lms.core.quiz3.app.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ai.lms.core.quiz3.app.service.crudquiz.CreateAnswerService;
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
import edu.ai.lms.core.quiz3.app.domain.CurrentActor;
import edu.ai.lms.core.quiz3.adapter.in.http.response.CreateAnswerResponse;
import edu.ai.lms.core.quiz3.app.domain.Answer;
import edu.ai.lms.core.quiz3.app.domain.Question;
import edu.ai.lms.core.quiz3.app.domain.Quiz;
import edu.ai.lms.core.quiz3.app.exception.QuestionNotFoundException;
import edu.ai.lms.core.quiz3.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz3.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz3.app.port.in.command.CreateAnswerCommand;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz3.app.port.out.SaveAnswerPort;

@ExtendWith(MockitoExtension.class)
class CreateAnswerServiceUT {

    @Mock
    private SaveAnswerPort saveAnswerPort;

    @Mock
    private LoadQuestionPort loadQuestionPort;

    @Mock
    private LoadQuizPort loadQuizPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private CreateAnswerService createAnswerService;

    private CurrentActor mockTeacher;
    private UUID teacherId;
    private UUID quizId;
    private UUID questionId;
    private Quiz existingQuiz;
    private Question existingQuestion;
    private CreateAnswerCommand validCommand;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        quizId = UUID.randomUUID();
        questionId = UUID.randomUUID();

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

        Answer answer = Answer.builder()
                .content("A programming language")
                .isCorrect(true)
                .explanation("Java is a widely-used programming language")
                .build();

        validCommand = CreateAnswerCommand.builder()
                .questionId(questionId)
                .answer(answer)
                .build();
    }

    @Test
    @DisplayName("Should successfully create an answer with valid details")
    void testCreateAnswer_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

        Answer savedAnswer = Answer.builder()
                .answerNumber(1L)
                .content("A programming language")
                .isCorrect(true)
                .explanation("Java is a widely-used programming language")
                .questionId(questionId)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveAnswerPort.save(any(Answer.class))).thenReturn(savedAnswer);

        // Act
        CreateAnswerResponse response = createAnswerService.createAnswer(validCommand);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getAnswerNumber());
        assertEquals("A programming language", response.getContent());
        assertTrue(response.isCorrect());
        assertEquals("Java is a widely-used programming language", response.getExplanation());
        assertEquals(questionId, response.getQuestionId());

        verify(loadSignedInUserPort, times(1)).getAuthenticatedUser();
        verify(loadQuestionPort, times(1)).getByIdWithoutAnswers(questionId);
        verify(loadQuizPort, times(1)).getById(quizId);
        verify(saveAnswerPort, times(1)).save(any(Answer.class));
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testCreateAnswer_WithUnauthenticatedUser_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> createAnswerService.createAnswer(validCommand));

        assertEquals("User not authenticated", exception.getMessage());

        verify(saveAnswerPort, never()).save(any(Answer.class));
    }

    @Test
    @DisplayName("Should throw QuestionNotFoundException when question does not exist")
    void testCreateAnswer_WithNonExistentQuestion_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.empty());

        // Act & Assert
        QuestionNotFoundException exception = assertThrows(QuestionNotFoundException.class,
                () -> createAnswerService.createAnswer(validCommand));

        assertEquals("Question not found with id: " + questionId, exception.getMessage());

        verify(saveAnswerPort, never()).save(any(Answer.class));
    }

    @Test
    @DisplayName("Should throw QuizNotFoundException when quiz does not exist")
    void testCreateAnswer_WithNonExistentQuiz_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.empty());

        // Act & Assert
        QuizNotFoundException exception = assertThrows(QuizNotFoundException.class,
                () -> createAnswerService.createAnswer(validCommand));

        assertEquals("Quiz not found", exception.getMessage());

        verify(saveAnswerPort, never()).save(any(Answer.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not the quiz owner")
    void testCreateAnswer_WithNonOwner_ShouldThrowException() {
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
                () -> createAnswerService.createAnswer(validCommand));

        assertEquals("User is not authorized to add answers to this quiz's questions", exception.getMessage());

        verify(saveAnswerPort, never()).save(any(Answer.class));
    }

    @Test
    @DisplayName("Should set questionId on answer before saving")
    void testCreateAnswer_ShouldSetQuestionId() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

        ArgumentCaptor<Answer> answerCaptor = ArgumentCaptor.forClass(Answer.class);
        Answer savedAnswer = Answer.builder()
                .answerNumber(1L)
                .content("A programming language")
                .isCorrect(true)
                .questionId(questionId)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveAnswerPort.save(any(Answer.class))).thenReturn(savedAnswer);

        // Act
        createAnswerService.createAnswer(validCommand);

        // Assert
        verify(saveAnswerPort).save(answerCaptor.capture());
        Answer capturedAnswer = answerCaptor.getValue();
        assertEquals(questionId, capturedAnswer.getQuestionId());
    }
}
