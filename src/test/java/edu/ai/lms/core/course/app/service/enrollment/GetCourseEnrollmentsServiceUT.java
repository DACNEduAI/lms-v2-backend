package edu.ai.lms.core.course.app.service.enrollment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.GetCourseEnrollmentsResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.LoadUsersPort;
import edu.ai.lms.core.course.app.port.out.UserSummaryView;
import edu.ai.lms.core.course.adapter.in.response.ParticipantDto;

@ExtendWith(MockitoExtension.class)
class GetCourseEnrollmentsServiceUT {

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private LoadUsersPort loadUsersPort;

    @InjectMocks
    private GetCourseEnrollmentsService getCourseEnrollmentsService;

    private UUID teacherId;
    private UUID courseId;
    private CurrentActor mockTeacher;
    private Course mockCourse;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        mockTeacher = CurrentActor.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        mockCourse = Course.builder()
                .id(courseId)
                .courseName("Enrollment Test Course")
                .courseCode("ETC-101")
                .teacherIds(List.of(teacherId))
                .studentIds(List.of())
                .build();
    }

    @Test
    @DisplayName("Should return teachers and students enrolled in a course")
    void testGetCourseEnrollments_Success() {
        UUID studentId = UUID.randomUUID();
        mockCourse.setStudentIds(List.of(studentId));

        UserSummaryView teacherSummary = new UserSummaryView(
                teacherId, "teacher@example.com", "Martin", "Fowler", null, Roles.TEACHER);
        UserSummaryView studentSummary = new UserSummaryView(
                studentId, null, "Alice", "Chen", null, Roles.STUDENT);

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadUsersPort.findAllByIds(List.of(teacherId))).thenReturn(List.of(teacherSummary));
        when(loadUsersPort.findAllByIds(List.of(studentId))).thenReturn(List.of(studentSummary));

        GetCourseEnrollmentsResponse response = getCourseEnrollmentsService.getCourseEnrollments(courseId);

        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals(1, response.getTeachers().size());
        assertEquals(1, response.getStudents().size());
        assertEquals(teacherId, response.getTeachers().get(0).getId());
        assertEquals(studentId, response.getStudents().get(0).getId());
        assertEquals("Martin", response.getTeachers().get(0).getFirstName());
        assertEquals("Alice", response.getStudents().get(0).getFirstName());
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher is not authenticated")
    void testGetCourseEnrollments_TeacherNotAuthenticated() {
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        assertThrows(TeacherNotFoundException.class,
                () -> getCourseEnrollmentsService.getCourseEnrollments(courseId));

        verify(loadCoursePort, never()).findById(courseId);
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testGetCourseEnrollments_CourseNotFound() {
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class,
                () -> getCourseEnrollmentsService.getCourseEnrollments(courseId));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher is not course owner")
    void testGetCourseEnrollments_UnauthorizedTeacher() {
        UUID anotherTeacherId = UUID.randomUUID();
        CurrentActor anotherTeacher = CurrentActor.builder()
                .id(anotherTeacherId)
                .role(Roles.TEACHER)
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(anotherTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> getCourseEnrollmentsService.getCourseEnrollments(courseId));

        assertTrue(ex.getMessage().contains("not authorized"));
    }
}
