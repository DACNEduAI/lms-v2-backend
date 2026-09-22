package edu.ai.lms.core.quiz3.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.ai.lms.core.quiz3.app.service.crudquiz.GetAllQuizzesService;
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

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz3.app.domain.CurrentActor;
import edu.ai.lms.core.quiz3.adapter.in.http.response.GetAllQuizzesResponse;
import edu.ai.lms.core.quiz3.app.domain.Quiz;
import edu.ai.lms.core.quiz3.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz3.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadSignedInUserPort;

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

    private CurrentActor mockTeacher;
    private CurrentActor mockStudent;
    private UUID teacherId;
    private UUID studentId;
    private UUID courseId;
    private List<Quiz> teacherQuizzes;
    private List<Quiz> studentQuizzes;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        mockTeacher = CurrentActor.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        mockStudent = CurrentActor.builder()
                .id(studentId)
                .role(Roles.STUDENT)
                .build();

        teacherQuizzes = List.of(
                Quiz.builder()
                        .id(UUID.randomUUID())
                        .quizTitle("Alpha Quiz")
                        .courseId(courseId)
                        .teacherId(teacherId)
                        .display(true)
                        .createdAt(LocalDateTime.of(2025, 1, 10, 9, 0))
                        .build(),
                Quiz.builder()
                        .id(UUID.randomUUID())
                        .quizTitle("Beta Quiz")
                        .courseId(courseId)
                        .teacherId(teacherId)
                        .display(false)
                        .createdAt(LocalDateTime.of(2025, 2, 10, 9, 0))
                        .build(),
                Quiz.builder()
                        .id(UUID.randomUUID())
                        .quizTitle("Charlie Quiz")
                        .courseId(courseId)
                        .teacherId(teacherId)
                        .display(true)
                        .createdAt(LocalDateTime.of(2025, 3, 10, 9, 0))
                        .build());

        studentQuizzes = List.of(
                Quiz.builder()
                        .id(UUID.randomUUID())
                        .quizTitle("Student Quiz 1")
                        .courseId(courseId)
                        .studentId(studentId)
                        .createdAt(LocalDateTime.of(2025, 4, 10, 9, 0))
                        .build(),
                Quiz.builder()
                        .id(UUID.randomUUID())
                        .quizTitle("Student Quiz 2")
                        .courseId(courseId)
                        .studentId(studentId)
                        .createdAt(LocalDateTime.of(2025, 5, 10, 9, 0))
                        .build());
    }

        @Test
        @DisplayName("Should return all teacher-created quizzes for teacher")
    void testGetAllQuizzesCreatedByTeacher_ShouldSucceed() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);
        when(loadQuizPort.getTeacherCreatedByCourseId(courseId)).thenReturn(teacherQuizzes);

        GetAllQuizzesResponse response = getAllQuizzesService.getAllQuizzesCreatedByTeacher(courseId, 10, "asc");

        assertNotNull(response);
        assertEquals(3, response.getTotalQuizzes());
        assertEquals(3, response.getQuizzes().size());
        assertEquals("Alpha Quiz", response.getQuizzes().get(0).getQuizTitle());
        assertEquals("Beta Quiz", response.getQuizzes().get(1).getQuizTitle());
        assertEquals("Charlie Quiz", response.getQuizzes().get(2).getQuizTitle());
        verify(loadQuizPort, times(1)).getTeacherCreatedByCourseId(courseId);
        verify(loadQuizPort, never()).getStudentCreatedByCourseIdAndStudentId(courseId, teacherId);
    }

        @Test
        @DisplayName("Should allow student to view displayable teacher-created quizzes")
        void testGetAllQuizzesCreatedByTeacher_AsStudent_ShouldSucceed() {
                when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
                when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
                when(loadQuizPort.getTeacherCreatedByCourseId(courseId)).thenReturn(teacherQuizzes);

                GetAllQuizzesResponse response = getAllQuizzesService.getAllQuizzesCreatedByTeacher(courseId, 10, "asc");

                assertNotNull(response);
                assertEquals(2, response.getTotalQuizzes());
                assertEquals(2, response.getQuizzes().size());
                assertEquals("Alpha Quiz", response.getQuizzes().get(0).getQuizTitle());
                assertEquals("Charlie Quiz", response.getQuizzes().get(1).getQuizTitle());
        }

    @Test
    @DisplayName("Should return only student-created quizzes for authenticated student")
    void testGetAllQuizzesCreatedByStudent_ShouldSucceed() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
        when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizPort.getStudentCreatedByCourseIdAndStudentId(courseId, studentId)).thenReturn(studentQuizzes);

        GetAllQuizzesResponse response = getAllQuizzesService.getAllQuizzesCreatedByStudent(courseId, 10, "asc");

        assertNotNull(response);
        assertEquals(2, response.getTotalQuizzes());
        assertEquals(2, response.getQuizzes().size());
        assertEquals("Student Quiz 1", response.getQuizzes().get(0).getQuizTitle());
        verify(loadQuizPort, times(1)).getStudentCreatedByCourseIdAndStudentId(courseId, studentId);
        verify(loadQuizPort, never()).getTeacherCreatedByCourseId(courseId);
    }

    @Test
    @DisplayName("Should return teacher quizzes in descending created-at order")
    void testGetAllQuizzesCreatedByTeacher_WithDesSort_ShouldReturnDescendingByCreatedAt() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);
        when(loadQuizPort.getTeacherCreatedByCourseId(courseId)).thenReturn(teacherQuizzes);

        GetAllQuizzesResponse response = getAllQuizzesService.getAllQuizzesCreatedByTeacher(courseId, 10, "des");

        assertNotNull(response);
        assertEquals("des", response.getLastCreated());
        assertEquals("Charlie Quiz", response.getQuizzes().get(0).getQuizTitle());
                assertEquals("Beta Quiz", response.getQuizzes().get(1).getQuizTitle());
                assertEquals("Alpha Quiz", response.getQuizzes().get(2).getQuizTitle());
    }

    @Test
    @DisplayName("Should default lastCreated to asc when null for student endpoint")
    void testGetAllQuizzesCreatedByStudent_WithNullLastCreated_ShouldDefaultToAsc() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
        when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizPort.getStudentCreatedByCourseIdAndStudentId(courseId, studentId)).thenReturn(studentQuizzes);

        GetAllQuizzesResponse response = getAllQuizzesService.getAllQuizzesCreatedByStudent(courseId, 10, null);

        assertNotNull(response);
        assertEquals("asc", response.getLastCreated());
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testGetAllQuizzesCreatedByTeacher_WithUnauthenticatedUser_ShouldThrowException() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.empty());

        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> getAllQuizzesService.getAllQuizzesCreatedByTeacher(courseId, 10, "asc"));

        assertEquals("User not authenticated", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when student endpoint called by teacher")
    void testGetAllQuizzesCreatedByStudent_ByTeacher_ShouldThrowException() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> getAllQuizzesService.getAllQuizzesCreatedByStudent(courseId, 10, "asc"));

        assertEquals("Only students can view student-created quizzes", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher is not a member of the course")
    void testGetAllQuizzesCreatedByTeacher_WithNonMember_ShouldThrowException() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> getAllQuizzesService.getAllQuizzesCreatedByTeacher(courseId, 10, "asc"));

        assertEquals("User is not a member of this course", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when student is not a member of the course")
    void testGetAllQuizzesCreatedByStudent_WithNonMember_ShouldThrowException() {
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
        when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> getAllQuizzesService.getAllQuizzesCreatedByStudent(courseId, 10, "asc"));

        assertEquals("User is not a member of this course", exception.getMessage());
    }
}
