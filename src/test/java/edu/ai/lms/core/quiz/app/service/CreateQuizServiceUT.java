package edu.ai.lms.core.quiz.app.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import edu.ai.lms.core.quiz.adapter.in.http.response.CreateQuizResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuizCommand;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizPort;

@ExtendWith(MockitoExtension.class)
class CreateQuizServiceUT {

    @Mock
    private SaveQuizPort saveQuizPort;

    @Mock
    private SaveQuestionPort saveQuestionPort;

    @Mock
    private SaveAnswerPort saveAnswerPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private LoadCourseMembershipPort loadCourseMembershipPort;

    @InjectMocks
    private CreateQuizService createQuizService;

    private User mockTeacher;
    private User mockStudent;
    private UUID teacherId;
    private UUID studentId;
    private UUID courseId;
    private UUID quizId;
    private CreateQuizCommand validCommand;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        quizId = UUID.randomUUID();

        mockTeacher = User.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        mockStudent = User.builder()
                .id(studentId)
                .role(Roles.STUDENT)
                .build();

        Quiz quiz = Quiz.builder()
                .quizTitle("Midterm Quiz")
                .timeLimitMinutes(60)
                .passScore(70.0)
                .build();

        validCommand = CreateQuizCommand.builder()
                .courseId(courseId)
                .quiz(quiz)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a quiz as a teacher")
    void testCreateQuiz_AsTeacher_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);

        Quiz savedQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Midterm Quiz")
                .timeLimitMinutes(60)
                .passScore(70.0)
                .courseId(courseId)
                .teacherId(teacherId)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveQuizPort.save(any(Quiz.class))).thenReturn(savedQuiz);

        // Act
        CreateQuizResponse response = createQuizService.createQuiz(validCommand);

        // Assert
        assertNotNull(response);
        assertEquals(quizId, response.getQuizId());
        assertEquals("Midterm Quiz", response.getQuizTitle());
        assertEquals(60, response.getTimeLimitMinutes());
        assertEquals(70.0, response.getPassScore());
        assertEquals(courseId, response.getCourseId());
        assertTrue(response.getActive());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCourseMembershipPort, times(1)).isTeacherInCourse(teacherId, courseId);
        verify(saveQuizPort, times(1)).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Should successfully create a quiz as a student")
    void testCreateQuiz_AsStudent_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
        when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);

        Quiz savedQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Midterm Quiz")
                .timeLimitMinutes(60)
                .passScore(70.0)
                .courseId(courseId)
                .studentId(studentId)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveQuizPort.save(any(Quiz.class))).thenReturn(savedQuiz);

        // Act
        CreateQuizResponse response = createQuizService.createQuiz(validCommand);

        // Assert
        assertNotNull(response);
        assertEquals(quizId, response.getQuizId());
        assertEquals(courseId, response.getCourseId());

        verify(loadCourseMembershipPort, times(1)).isStudentInCourse(studentId, courseId);
        verify(saveQuizPort, times(1)).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testCreateQuiz_WithUnauthenticatedUser_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> createQuizService.createQuiz(validCommand));

        assertEquals("User not authenticated", exception.getMessage());

        verify(saveQuizPort, never()).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher is not a member of the course")
    void testCreateQuiz_WithNonMemberTeacher_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createQuizService.createQuiz(validCommand));

        assertEquals("User is not a member of this course", exception.getMessage());

        verify(saveQuizPort, never()).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Should set courseId and teacherId on quiz before saving")
    void testCreateQuiz_ShouldSetCourseIdAndTeacherId() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);

        ArgumentCaptor<Quiz> quizCaptor = ArgumentCaptor.forClass(Quiz.class);
        Quiz savedQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Midterm Quiz")
                .courseId(courseId)
                .teacherId(teacherId)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveQuizPort.save(any(Quiz.class))).thenReturn(savedQuiz);

        // Act
        createQuizService.createQuiz(validCommand);

        // Assert
        verify(saveQuizPort).save(quizCaptor.capture());
        Quiz capturedQuiz = quizCaptor.getValue();
        assertEquals(courseId, capturedQuiz.getCourseId());
        assertEquals(teacherId, capturedQuiz.getTeacherId());
    }

    @Test
    @DisplayName("Should create quiz with nested questions and answers")
    void testCreateQuiz_WithNestedQuestionsAndAnswers_ShouldSucceed() {
        // Arrange
        UUID questionId = UUID.randomUUID();
        Answer answer = Answer.builder()
                .content("Paris")
                .isCorrect(true)
                .explanation("Paris is the capital of France")
                .build();

        Question question = Question.builder()
                .content("What is the capital of France?")
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .points(10.0)
                .answers(new ArrayList<>(List.of(answer)))
                .build();

        Quiz quizWithQuestions = Quiz.builder()
                .quizTitle("Geography Quiz")
                .timeLimitMinutes(30)
                .passScore(60.0)
                .questions(new ArrayList<>(List.of(question)))
                .build();

        CreateQuizCommand commandWithQuestions = CreateQuizCommand.builder()
                .courseId(courseId)
                .quiz(quizWithQuestions)
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);

        Quiz savedQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Geography Quiz")
                .timeLimitMinutes(30)
                .passScore(60.0)
                .courseId(courseId)
                .teacherId(teacherId)
                .active(true)
                .questions(new ArrayList<>(List.of(question)))
                .createdAt(LocalDateTime.now())
                .build();
        when(saveQuizPort.save(any(Quiz.class))).thenReturn(savedQuiz);

        Question savedQuestion = Question.builder()
                .id(questionId)
                .content("What is the capital of France?")
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .points(10.0)
                .quizId(quizId)
                .answers(new ArrayList<>(List.of(answer)))
                .build();
        when(saveQuestionPort.save(any(Question.class))).thenReturn(savedQuestion);

        Answer savedAnswer = Answer.builder()
                .answerNumber(1L)
                .content("Paris")
                .isCorrect(true)
                .explanation("Paris is the capital of France")
                .questionId(questionId)
                .build();
        when(saveAnswerPort.save(any(Answer.class))).thenReturn(savedAnswer);

        // Act
        CreateQuizResponse response = createQuizService.createQuiz(commandWithQuestions);

        // Assert
        assertNotNull(response);
        assertEquals(quizId, response.getQuizId());
        assertNotNull(response.getQuestions());
        assertEquals(1, response.getQuestions().size());
        assertEquals(questionId, response.getQuestions().get(0).getQuestionId());
        assertEquals("What is the capital of France?", response.getQuestions().get(0).getContent());
        assertEquals(1, response.getQuestions().get(0).getAnswers().size());
        assertEquals(1L, response.getQuestions().get(0).getAnswers().get(0).getAnswerNumber());
        assertEquals("Paris", response.getQuestions().get(0).getAnswers().get(0).getContent());

        verify(saveQuizPort, times(1)).save(any(Quiz.class));
        verify(saveQuestionPort, times(1)).save(any(Question.class));
        verify(saveAnswerPort, times(1)).save(any(Answer.class));
    }

    @Test
    @DisplayName("Should create quiz without questions when questions list is empty")
    void testCreateQuiz_WithEmptyQuestions_ShouldNotSaveQuestions() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);

        Quiz savedQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Midterm Quiz")
                .courseId(courseId)
                .teacherId(teacherId)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveQuizPort.save(any(Quiz.class))).thenReturn(savedQuiz);

        // Act
        CreateQuizResponse response = createQuizService.createQuiz(validCommand);

        // Assert
        assertNotNull(response);
        assertTrue(response.getQuestions().isEmpty());

        verify(saveQuizPort, times(1)).save(any(Quiz.class));
        verify(saveQuestionPort, never()).save(any(Question.class));
        verify(saveAnswerPort, never()).save(any(Answer.class));
    }
}
