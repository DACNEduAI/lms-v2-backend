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
import edu.ai.lms.core.quiz.adapter.in.http.response.CreateQuestionResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuestionCommand;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;

@ExtendWith(MockitoExtension.class)
class CreateQuestionServiceUT {

    @Mock
    private SaveQuestionPort saveQuestionPort;

    @Mock
    private SaveAnswerPort saveAnswerPort;

    @Mock
    private LoadQuizPort loadQuizPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private CreateQuestionService createQuestionService;

    private UserAuth mockTeacher;
    private UUID teacherId;
    private UUID quizId;
    private UUID questionId;
    private Quiz existingQuiz;
    private CreateQuestionCommand validCommand;

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
                .active(true)
                .build();

        Answer answer = Answer.builder()
                .content("Java is a programming language")
                .isCorrect(true)
                .explanation("Correct")
                .build();

        Question question = Question.builder()
                .content("What is Java?")
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .points(10.0)
                .answers(new ArrayList<>(List.of(answer)))
                .build();

        validCommand = CreateQuestionCommand.builder()
                .quizId(quizId)
                .question(question)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a question with answers")
    void testCreateQuestion_WithValidDetails_ShouldSucceed() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

        Question savedQuestion = Question.builder()
                .id(questionId)
                .content("What is Java?")
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .points(10.0)
                .quizId(quizId)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveQuestionPort.save(any(Question.class))).thenReturn(savedQuestion);

        Answer savedAnswer = Answer.builder()
                .answerNumber(1L)
                .content("Java is a programming language")
                .isCorrect(true)
                .explanation("Correct")
                .questionId(questionId)
                .build();
        when(saveAnswerPort.save(any(Answer.class))).thenReturn(savedAnswer);

        CreateQuestionResponse response = createQuestionService.createQuestion(validCommand);

        assertNotNull(response);
        assertEquals(questionId, response.getQuestionId());
        assertEquals(1, response.getAnswers().size());
        assertEquals(1L, response.getAnswers().get(0).getAnswerNumber());
        verify(saveQuestionPort, times(1)).save(any(Question.class));
        verify(saveAnswerPort, times(1)).save(any(Answer.class));
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testCreateQuestion_WithUnauthenticatedUser_ShouldThrowException() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.empty());

        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> createQuestionService.createQuestion(validCommand));

        assertEquals("User not authenticated", exception.getMessage());
        verify(saveQuestionPort, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("Should throw QuizNotFoundException when quiz does not exist")
    void testCreateQuestion_WithNonExistentQuiz_ShouldThrowException() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.empty());

        QuizNotFoundException exception = assertThrows(QuizNotFoundException.class,
                () -> createQuestionService.createQuestion(validCommand));

        assertEquals("Quiz not found with id: " + quizId, exception.getMessage());
        verify(saveQuestionPort, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not the quiz owner")
    void testCreateQuestion_WithNonOwner_ShouldThrowException() {
        UUID anotherTeacherId = UUID.randomUUID();
        UserAuth anotherTeacher = UserAuth.builder()
                .id(anotherTeacherId)
                .role(Roles.TEACHER)
                .build();

        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(anotherTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createQuestionService.createQuestion(validCommand));

        assertEquals("User is not authorized to add questions to this quiz", exception.getMessage());
        verify(saveQuestionPort, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("Should set quizId on question before saving")
    void testCreateQuestion_ShouldSetQuizId() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadQuizPort.getById(quizId)).thenReturn(Optional.of(existingQuiz));

        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        Question savedQuestion = Question.builder()
                .id(questionId)
                .content("What is Java?")
                .quizId(quizId)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveQuestionPort.save(any(Question.class))).thenReturn(savedQuestion);
        when(saveAnswerPort.save(any(Answer.class))).thenReturn(Answer.builder().answerNumber(1L).build());

        createQuestionService.createQuestion(validCommand);

        verify(saveQuestionPort).save(questionCaptor.capture());
        Question capturedQuestion = questionCaptor.getValue();
        assertEquals(quizId, capturedQuestion.getQuizId());
    }
}
