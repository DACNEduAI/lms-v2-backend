package edu.ai.lms.core.quiz.app.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuestionResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.exception.QuestionNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.common.Roles;

@ExtendWith(MockitoExtension.class)
class GetQuestionServiceUT {

    @Mock
    private LoadQuestionPort loadQuestionPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private GetQuestionService getQuestionService;

    private UserAuth mockTeacher;
    private UUID teacherId;
    private UUID quizId;
    private UUID questionId;
    private Question existingQuestion;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        quizId = UUID.randomUUID();
        questionId = UUID.randomUUID();

        mockTeacher = UserAuth.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        Answer answer1 = Answer.builder()
                .answerNumber(1L)
                .content("Paris")
                .isCorrect(true)
                .explanation("Paris is the capital of France")
                .questionId(questionId)
                .build();

        Answer answer2 = Answer.builder()
                .answerNumber(2L)
                .content("London")
                .isCorrect(false)
                .explanation("London is the capital of England")
                .questionId(questionId)
                .build();

        existingQuestion = Question.builder()
                .id(questionId)
                .content("What is the capital of France?")
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .points(10.0)
                .quizId(quizId)
                .answers(new ArrayList<>(List.of(answer1, answer2)))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully get a question with answers")
    void testGetQuestion_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getById(questionId)).thenReturn(Optional.of(existingQuestion));

        // Act
        GetQuestionResponse response = getQuestionService.getQuestion(quizId, questionId);

        // Assert
        assertNotNull(response);
        assertEquals(questionId, response.getQuestionId());
        assertEquals("What is the capital of France?", response.getContent());
        assertEquals("MULTIPLE_CHOICE", response.getType());
        assertEquals(10.0, response.getPoints());
        assertEquals(quizId, response.getQuizId());
        assertNotNull(response.getAnswers());
        assertEquals(2, response.getAnswers().size());
        assertEquals("Paris", response.getAnswers().get(0).getContent());
        assertEquals("London", response.getAnswers().get(1).getContent());

        verify(loadSignedInUserPort, times(1)).getAuthenticatedUser();
        verify(loadQuestionPort, times(1)).getById(questionId);
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testGetQuestion_WithUnauthenticatedUser_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> getQuestionService.getQuestion(quizId, questionId));

        assertEquals("User not authenticated", exception.getMessage());

        verify(loadQuestionPort, never()).getById(questionId);
    }

    @Test
    @DisplayName("Should throw QuestionNotFoundException when question does not exist")
    void testGetQuestion_WithNonExistentQuestion_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getById(questionId)).thenReturn(Optional.empty());

        // Act & Assert
        QuestionNotFoundException exception = assertThrows(QuestionNotFoundException.class,
                () -> getQuestionService.getQuestion(quizId, questionId));

        assertEquals("Question not found with id: " + questionId, exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when question does not belong to the quiz")
    void testGetQuestion_WithQuizMismatch_ShouldThrowException() {
        // Arrange
        UUID differentQuizId = UUID.randomUUID();
        existingQuestion.setQuizId(differentQuizId);

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getById(questionId)).thenReturn(Optional.of(existingQuestion));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> getQuestionService.getQuestion(quizId, questionId));

        assertEquals("Question does not belong to this quiz", exception.getMessage());
    }

    @Test
    @DisplayName("Should return question with empty answers list")
    void testGetQuestion_WithNoAnswers_ShouldReturnEmptyAnswers() {
        // Arrange
        existingQuestion.setAnswers(new ArrayList<>());

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuestionPort.getById(questionId)).thenReturn(Optional.of(existingQuestion));

        // Act
        GetQuestionResponse response = getQuestionService.getQuestion(quizId, questionId);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getAnswers());
        assertEquals(0, response.getAnswers().size());
    }
}
