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
import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyQuestionResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuestionNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuestionCommand;
import edu.ai.lms.core.quiz.app.port.out.DeleteAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.LoadAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;

@ExtendWith(MockitoExtension.class)
class ModifyQuestionServiceUT {

    @Mock
    private SaveQuestionPort saveQuestionPort;

    @Mock
    private SaveAnswerPort saveAnswerPort;

    @Mock
    private LoadAnswerPort loadAnswerPort;

    @Mock
    private DeleteAnswerPort deleteAnswerPort;

    @Mock
    private LoadQuestionPort loadQuestionPort;

    @Mock
    private LoadQuizPort loadQuizPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private ModifyQuestionService modifyQuestionService;

    private UserAuth mockTeacher;
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
                .content("Original Question")
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .quizId(quizId)
                .createdAt(LocalDateTime.now())
                .build();

        Answer updatedAnswer = Answer.builder()
                .content("Updated answer")
                .isCorrect(true)
                .explanation("Updated explanation")
                .build();

        Question updatedQuestion = Question.builder()
                .content("Updated Question")
                .type(Question.QuestionType.TRUE_FALSE)
                .answers(new ArrayList<>(List.of(updatedAnswer)))
                .build();

        validCommand = ModifyQuestionCommand.builder()
                .quizId(quizId)
                .questionId(questionId)
                .question(updatedQuestion)
                .build();
    }

    @Test
    @DisplayName("Should successfully modify a question with answers")
    void testModifyQuestion_WithValidDetails_ShouldSucceed() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));
        when(loadAnswerPort.getByQuestionId(questionId)).thenReturn(List.of(
                Answer.builder().answerNumber(1L).questionId(questionId).build()));

        Question savedQuestion = Question.builder()
                .id(questionId)
                .content("Updated Question")
                .type(Question.QuestionType.TRUE_FALSE)
                .quizId(quizId)
                .updatedAt(LocalDateTime.now())
                .build();
        when(saveQuestionPort.save(any(Question.class))).thenReturn(savedQuestion);

        Answer savedAnswer = Answer.builder()
                .answerNumber(2L)
                .content("Updated answer")
                .isCorrect(true)
                .explanation("Updated explanation")
                .questionId(questionId)
                .build();
        when(saveAnswerPort.save(any(Answer.class))).thenReturn(savedAnswer);

        ModifyQuestionResponse response = modifyQuestionService.modifyQuestion(validCommand);

        assertNotNull(response);
        assertEquals(questionId, response.getQuestionId());
        assertEquals(1, response.getAnswers().size());
        assertEquals(2L, response.getAnswers().get(0).getAnswerNumber());
        verify(deleteAnswerPort, times(1)).deleteByKey(questionId, 1L);
        verify(saveAnswerPort, times(1)).save(any(Answer.class));
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testModifyQuestion_WithUnauthenticatedUser_ShouldThrowException() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.empty());

        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> modifyQuestionService.modifyQuestion(validCommand));

        assertEquals("User not authenticated", exception.getMessage());
        verify(saveQuestionPort, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("Should throw QuizNotFoundException when quiz does not exist")
    void testModifyQuestion_WithNonExistentQuiz_ShouldThrowException() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.empty());

        QuizNotFoundException exception = assertThrows(QuizNotFoundException.class,
                () -> modifyQuestionService.modifyQuestion(validCommand));

        assertEquals("Quiz not found with id: " + quizId, exception.getMessage());
        verify(saveQuestionPort, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not the quiz owner")
    void testModifyQuestion_WithNonOwner_ShouldThrowException() {
        UUID anotherTeacherId = UUID.randomUUID();
        UserAuth anotherTeacher = UserAuth.builder()
                .id(anotherTeacherId)
                .role(Roles.TEACHER)
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(anotherTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> modifyQuestionService.modifyQuestion(validCommand));

        assertEquals("User is not authorized to modify questions in this quiz", exception.getMessage());
        verify(saveQuestionPort, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("Should throw QuestionNotFoundException when question does not exist")
    void testModifyQuestion_WithNonExistentQuestion_ShouldThrowException() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.empty());

        QuestionNotFoundException exception = assertThrows(QuestionNotFoundException.class,
                () -> modifyQuestionService.modifyQuestion(validCommand));

        assertEquals("Question not found with id: " + questionId, exception.getMessage());
        verify(saveQuestionPort, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("Should preserve existing question id and quizId when modifying")
    void testModifyQuestion_ShouldPreserveExistingProperties() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));
        when(loadQuestionPort.getByIdWithoutAnswers(questionId)).thenReturn(Optional.of(existingQuestion));
        when(loadAnswerPort.getByQuestionId(questionId)).thenReturn(List.of());

        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        Question savedQuestion = Question.builder()
                .id(questionId)
                .content("Updated Question")
                .quizId(quizId)
                .updatedAt(LocalDateTime.now())
                .build();
        when(saveQuestionPort.save(any(Question.class))).thenReturn(savedQuestion);
        when(saveAnswerPort.save(any(Answer.class))).thenReturn(Answer.builder().answerNumber(1L).build());

        modifyQuestionService.modifyQuestion(validCommand);

        verify(saveQuestionPort).save(questionCaptor.capture());
        Question capturedQuestion = questionCaptor.getValue();
        assertEquals(questionId, capturedQuestion.getId());
        assertEquals(quizId, capturedQuestion.getQuizId());
    }
}
