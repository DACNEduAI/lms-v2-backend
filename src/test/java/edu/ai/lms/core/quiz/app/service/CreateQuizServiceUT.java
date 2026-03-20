package edu.ai.lms.core.quiz.app.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.CreateQuizResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuizCommand;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizPort;

@ExtendWith(MockitoExtension.class)
class CreateQuizServiceUT {

    @Mock
    private SaveQuizPort saveQuizPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private LoadCourseMembershipPort loadCourseMembershipPort;

    @InjectMocks
    private CreateQuizService createQuizService;

    private UserAuth mockTeacher;
    private UserAuth mockStudent;
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

        mockTeacher = UserAuth.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        mockStudent = UserAuth.builder()
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
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);

        Quiz savedQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Midterm Quiz")
                .timeLimitMinutes(60)
                .passScore(70.0)
                .courseId(courseId)
                .teacherId(teacherId)
                .display(true)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveQuizPort.save(any(Quiz.class))).thenReturn(savedQuiz);

        CreateQuizResponse response = createQuizService.createQuiz(validCommand);

        assertNotNull(response);
        assertEquals(quizId, response.getQuizId());
        assertEquals(courseId, response.getCourseId());
        assertTrue(response.getDisplay());
        verify(saveQuizPort, times(1)).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Should successfully create a quiz as a student")
    void testCreateQuiz_AsStudent_ShouldSucceed() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
        when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);

                validCommand.quiz().setStartTime(LocalDateTime.of(2025, 1, 10, 9, 0));
                validCommand.quiz().setDueTime(LocalDateTime.of(2025, 1, 10, 10, 0));
                validCommand.quiz().setAttemptLimited(2);

        Quiz savedQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Midterm Quiz")
                .courseId(courseId)
                .studentId(studentId)
                .display(true)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveQuizPort.save(any(Quiz.class))).thenReturn(savedQuiz);

        CreateQuizResponse response = createQuizService.createQuiz(validCommand);

        assertNotNull(response);
        assertEquals(quizId, response.getQuizId());
        assertEquals(courseId, response.getCourseId());
        verify(saveQuizPort, times(1)).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testCreateQuiz_WithUnauthenticatedUser_ShouldThrowException() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.empty());

        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> createQuizService.createQuiz(validCommand));

        assertEquals("User not authenticated", exception.getMessage());
        verify(saveQuizPort, never()).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher is not a member of the course")
    void testCreateQuiz_WithNonMemberTeacher_ShouldThrowException() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createQuizService.createQuiz(validCommand));

        assertEquals("User is not a member of this course", exception.getMessage());
        verify(saveQuizPort, never()).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Should set courseId and teacherId on quiz before saving")
    void testCreateQuiz_ShouldSetCourseIdAndTeacherId() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);

        ArgumentCaptor<Quiz> quizCaptor = ArgumentCaptor.forClass(Quiz.class);
        Quiz savedQuiz = Quiz.builder()
                .id(quizId)
                .quizTitle("Midterm Quiz")
                .courseId(courseId)
                .teacherId(teacherId)
                .display(true)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveQuizPort.save(any(Quiz.class))).thenReturn(savedQuiz);

        createQuizService.createQuiz(validCommand);

        verify(saveQuizPort).save(quizCaptor.capture());
        Quiz capturedQuiz = quizCaptor.getValue();
        assertEquals(courseId, capturedQuiz.getCourseId());
        assertEquals(teacherId, capturedQuiz.getTeacherId());
    }

        @Test
        @DisplayName("Should clear startTime, dueTime and attemptLimited for student-created quiz")
        void testCreateQuiz_AsStudent_ShouldClearTeacherOnlyFields() {
                when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
                when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);

                validCommand.quiz().setStartTime(LocalDateTime.of(2025, 4, 1, 8, 0));
                validCommand.quiz().setDueTime(LocalDateTime.of(2025, 4, 1, 9, 0));
                validCommand.quiz().setAttemptLimited(3);

                ArgumentCaptor<Quiz> quizCaptor = ArgumentCaptor.forClass(Quiz.class);
                when(saveQuizPort.save(any(Quiz.class))).thenReturn(Quiz.builder().id(quizId).courseId(courseId).build());

                createQuizService.createQuiz(validCommand);

                verify(saveQuizPort).save(quizCaptor.capture());
                Quiz capturedQuiz = quizCaptor.getValue();
                assertEquals(studentId, capturedQuiz.getStudentId());
                assertNull(capturedQuiz.getStartTime());
                assertNull(capturedQuiz.getDueTime());
                assertNull(capturedQuiz.getAttemptLimited());
        }
}
