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
import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuizResponse;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;

@ExtendWith(MockitoExtension.class)
class GetQuizServiceUT {

    @Mock
    private LoadQuizPort loadQuizPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private LoadCourseMembershipPort loadCourseMembershipPort;

    @InjectMocks
    private GetQuizService getQuizService;

    private User mockTeacher;
    private User mockStudent;
    private UUID teacherId;
    private UUID studentId;
    private UUID courseId;
    private UUID quizId;
    private Quiz existingQuiz;

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

        Question question = Question.builder()
                .id(UUID.randomUUID())
                .content("Sample Question")
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .points(10.0)
                .quizId(quizId)
                .answers(new ArrayList<>())
                .build();

        existingQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Midterm Quiz")
                .timeLimitMinutes(60)
                .passScore(70.0)
                .courseId(courseId)
                .teacherId(teacherId)
                .active(true)
                .questions(new ArrayList<>(List.of(question)))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully get a quiz as the teacher owner")
    void testGetQuiz_AsTeacherOwner_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.of(existingQuiz));

        // Act
        GetQuizResponse response = getQuizService.getQuiz(courseId, quizId);

        // Assert
        assertNotNull(response);
        assertEquals(quizId, response.getQuizId());
        assertEquals("Midterm Quiz", response.getQuizTitle());
        assertEquals(60, response.getTimeLimitMinutes());
        assertEquals(70.0, response.getPassScore());
        assertEquals(courseId, response.getCourseId());
        assertNotNull(response.getQuestions());
        assertEquals(1, response.getQuestions().size());
        assertEquals("Sample Question", response.getQuestions().get(0).getContent());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadQuizPort, times(1)).findById(quizId);
    }

    @Test
    @DisplayName("Should successfully get a quiz as a student member")
    void testGetQuiz_AsStudentMember_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
        when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.of(existingQuiz));

        // Act
        GetQuizResponse response = getQuizService.getQuiz(courseId, quizId);

        // Assert
        assertNotNull(response);
        assertEquals(quizId, response.getQuizId());
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testGetQuiz_WithUnauthenticatedUser_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> getQuizService.getQuiz(courseId, quizId));

        assertEquals("User not authenticated", exception.getMessage());

        verify(loadQuizPort, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not a member of the course")
    void testGetQuiz_WithNonMember_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> getQuizService.getQuiz(courseId, quizId));

        assertEquals("User is not a member of this course", exception.getMessage());

        verify(loadQuizPort, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw QuizNotFoundException when quiz does not exist")
    void testGetQuiz_WithNonExistentQuiz_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.empty());

        // Act & Assert
        QuizNotFoundException exception = assertThrows(QuizNotFoundException.class,
                () -> getQuizService.getQuiz(courseId, quizId));

        assertEquals("Quiz not found with id: " + quizId, exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when quiz does not belong to the course")
    void testGetQuiz_WithCourseMismatch_ShouldThrowException() {
        // Arrange
        UUID differentCourseId = UUID.randomUUID();
        existingQuiz.setCourseId(differentCourseId);

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.of(existingQuiz));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> getQuizService.getQuiz(courseId, quizId));

        assertEquals("Quiz does not belong to this course", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher tries to access another teacher's quiz")
    void testGetQuiz_AsNonOwnerTeacher_ShouldThrowException() {
        // Arrange
        UUID anotherTeacherId = UUID.randomUUID();
        User anotherTeacher = User.builder()
                .id(anotherTeacherId)
                .role(Roles.TEACHER)
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(anotherTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(anotherTeacherId, courseId)).thenReturn(true);
        when(loadQuizPort.findById(quizId)).thenReturn(Optional.of(existingQuiz));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> getQuizService.getQuiz(courseId, quizId));

        assertEquals("Teacher is not authorized to access this quiz", exception.getMessage());
    }
}
