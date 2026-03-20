package edu.ai.lms.core.quiz.app.service;

import java.time.LocalDateTime;
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

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetAllQuizzesResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;

@ExtendWith(MockitoExtension.class)
class GetAllQuizzesServiceUT {

    @Mock
    private LoadQuizPort loadQuizPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private LoadCourseMembershipPort loadCourseMembershipPort;

    @InjectMocks
    private GetAllQuizzesService getAllQuizzesService;

    private UserAuth mockTeacher;
    private UserAuth mockStudent;
    private UUID teacherId;
    private UUID studentId;
    private UUID courseId;
    private List<Quiz> quizzes;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        mockTeacher = UserAuth.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        mockStudent = UserAuth.builder()
                .id(studentId)
                .role(Roles.STUDENT)
                .build();

        quizzes = List.of(
                Quiz.builder()
                        .id(UUID.randomUUID())
                        .quizTitle("Alpha Quiz")
                        .courseId(courseId)
                        .teacherId(teacherId)
                        .startTime(LocalDateTime.of(2025, 1, 10, 9, 0))
                        .display(true)
                        .build(),
                Quiz.builder()
                        .id(UUID.randomUUID())
                        .quizTitle("Beta Quiz")
                        .courseId(courseId)
                        .teacherId(teacherId)
                        .startTime(LocalDateTime.of(2025, 2, 10, 9, 0))
                        .display(false)
                        .build(),
                Quiz.builder()
                        .id(UUID.randomUUID())
                        .quizTitle("Charlie Quiz")
                        .courseId(courseId)
                        .teacherId(teacherId)
                        .startTime(LocalDateTime.of(2025, 3, 10, 9, 0))
                        .display(true)
                        .build());
    }

    @Test
    @DisplayName("Should return displayed teacher-created quizzes for student")
    void testGetAllQuizzes_AsStudent_ShouldReturnDisplayedTeacherCreatedQuizzes() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
        when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizPort.getDisplayedTeacherCreatedByCourseId(courseId)).thenReturn(quizzes);

        GetAllQuizzesResponse response = getAllQuizzesService.getAllQuizzes(courseId, 10, "asc");

        assertNotNull(response);
        assertEquals(3, response.getTotalQuizzes());
        assertEquals(3, response.getQuizzes().size());
        assertEquals("asc", response.getLastCreated());
        assertEquals("Alpha Quiz", response.getQuizzes().get(0).getQuizTitle());

        verify(loadQuizPort, times(1)).getDisplayedTeacherCreatedByCourseId(courseId);
        verify(loadQuizPort, never()).getTeacherCreatedByCourseId(courseId);
    }

    @Test
    @DisplayName("Should return all teacher-created quizzes for teacher")
    void testGetAllQuizzes_AsTeacher_ShouldReturnTeacherCreatedQuizzes() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);
        when(loadQuizPort.getTeacherCreatedByCourseId(courseId)).thenReturn(quizzes);

        GetAllQuizzesResponse response = getAllQuizzesService.getAllQuizzes(courseId, 10, "asc");

        assertNotNull(response);
        assertEquals(3, response.getTotalQuizzes());

        verify(loadQuizPort, times(1)).getTeacherCreatedByCourseId(courseId);
        verify(loadQuizPort, never()).getDisplayedTeacherCreatedByCourseId(courseId);
    }

    @Test
    @DisplayName("Should return quizzes in descending order when lastCreated is des")
    void testGetAllQuizzes_WithDesSort_ShouldReturnDescendingByStartTime() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
        when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizPort.getDisplayedTeacherCreatedByCourseId(courseId)).thenReturn(quizzes);

        GetAllQuizzesResponse response = getAllQuizzesService.getAllQuizzes(courseId, 10, "des");

        assertNotNull(response);
        assertEquals("des", response.getLastCreated());
        assertEquals("Charlie Quiz", response.getQuizzes().get(0).getQuizTitle());
        assertEquals("Beta Quiz", response.getQuizzes().get(1).getQuizTitle());
        assertEquals("Alpha Quiz", response.getQuizzes().get(2).getQuizTitle());
    }

    @Test
    @DisplayName("Should default lastCreated to asc when null")
    void testGetAllQuizzes_WithNullLastCreated_ShouldDefaultToAsc() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
        when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizPort.getDisplayedTeacherCreatedByCourseId(courseId)).thenReturn(quizzes);

        GetAllQuizzesResponse response = getAllQuizzesService.getAllQuizzes(courseId, 10, null);

        assertNotNull(response);
        assertEquals("asc", response.getLastCreated());
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testGetAllQuizzes_WithUnauthenticatedUser_ShouldThrowException() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.empty());

        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> getAllQuizzesService.getAllQuizzes(courseId, 10, "asc"));

        assertEquals("User not authenticated", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not a member of the course")
    void testGetAllQuizzes_WithNonMember_ShouldThrowException() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> getAllQuizzesService.getAllQuizzes(courseId, 10, "asc"));

        assertEquals("User is not a member of this course", exception.getMessage());
    }
}
