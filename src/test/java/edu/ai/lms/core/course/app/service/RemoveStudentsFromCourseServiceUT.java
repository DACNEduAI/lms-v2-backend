package edu.ai.lms.core.course.app.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.course.adapter.in.response.RemoveStudentsFromCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.StudentNotEnrolledInCourseException;
import edu.ai.lms.core.course.app.exception.StudentsNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.RemoveStudentsFromCourseCommand;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.LoadStudentsByEmailPort;
import edu.ai.lms.core.course.app.port.out.SaveCourseEnrollmentPort;

@ExtendWith(MockitoExtension.class)
class RemoveStudentsFromCourseServiceUT {

    @Mock
        private SaveCourseEnrollmentPort saveCourseEnrollmentPort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private LoadStudentsByEmailPort loadStudentsByEmailPort;

    @InjectMocks
    private RemoveStudentsFromCourseService removeStudentsFromCourseService;

    private UUID teacherId;
    private UUID courseId;
    private UserAuth mockTeacher;
    private Course mockCourse;
    private UUID student1Id;
    private UUID student2Id;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        student1Id = UUID.randomUUID();
        student2Id = UUID.randomUUID();

        mockTeacher = UserAuth.builder()
                .id(teacherId)
                .email("teacher@example.com")
                .role(Roles.TEACHER)
                .build();

        mockCourse = Course.builder()
                .id(courseId)
                .courseName("Test Course")
                .courseCode("TC101")
                .teacherIds(new ArrayList<>(List.of(teacherId)))
                .studentIds(new ArrayList<>(List.of(student1Id, student2Id)))
                .build();
    }

    @Test
    @DisplayName("Should successfully remove students from a course")
    void testRemoveStudentsFromCourse_Success() {
        String email1 = "student1@example.com";
        String email2 = "student2@example.com";

        Map<String, UUID> emailToIdMap = new HashMap<>();
        emailToIdMap.put(email1, student1Id);
        emailToIdMap.put(email2, student2Id);

        RemoveStudentsFromCourseCommand command = RemoveStudentsFromCourseCommand.builder()
                .courseId(courseId)
                .studentEmails(List.of(email1, email2))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadStudentsByEmailPort.findStudentIdsByEmails(List.of(email1, email2))).thenReturn(emailToIdMap);
        RemoveStudentsFromCourseResponse response = removeStudentsFromCourseService.removeStudentsFromCourse(command);

        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals(2, response.getTotalRemoved());
        assertEquals(2, response.getRemovedStudentEmails().size());
        verify(saveCourseEnrollmentPort).removeStudentsFromCourse(any(UUID.class), any(List.class));
    }

    @Test
    @DisplayName("Should successfully remove a single student from the course")
    void testRemoveStudentsFromCourse_SingleStudent() {
        String email1 = "student1@example.com";

        Map<String, UUID> emailToIdMap = new HashMap<>();
        emailToIdMap.put(email1, student1Id);

        RemoveStudentsFromCourseCommand command = RemoveStudentsFromCourseCommand.builder()
                .courseId(courseId)
                .studentEmails(List.of(email1))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadStudentsByEmailPort.findStudentIdsByEmails(List.of(email1))).thenReturn(emailToIdMap);
        RemoveStudentsFromCourseResponse response = removeStudentsFromCourseService.removeStudentsFromCourse(command);

        assertNotNull(response);
        assertEquals(1, response.getTotalRemoved());
        assertEquals("Students removed from course successfully", response.getMessage());
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher is not authenticated")
    void testRemoveStudentsFromCourse_TeacherNotAuthenticated() {
        RemoveStudentsFromCourseCommand command = RemoveStudentsFromCourseCommand.builder()
                .courseId(courseId)
                .studentEmails(List.of("student@example.com"))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        assertThrows(TeacherNotFoundException.class,
                () -> removeStudentsFromCourseService.removeStudentsFromCourse(command));

        verify(saveCourseEnrollmentPort, never()).removeStudentsFromCourse(any(UUID.class), any(List.class));
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testRemoveStudentsFromCourse_CourseNotFound() {
        RemoveStudentsFromCourseCommand command = RemoveStudentsFromCourseCommand.builder()
                .courseId(courseId)
                .studentEmails(List.of("student@example.com"))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class,
                () -> removeStudentsFromCourseService.removeStudentsFromCourse(command));

        verify(saveCourseEnrollmentPort, never()).removeStudentsFromCourse(any(UUID.class), any(List.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher does not own the course")
    void testRemoveStudentsFromCourse_UnauthorizedTeacher() {
        UUID anotherTeacherId = UUID.randomUUID();
        UserAuth anotherTeacher = UserAuth.builder()
                .id(anotherTeacherId)
                .email("other@example.com")
                .role(Roles.TEACHER)
                .build();

        RemoveStudentsFromCourseCommand command = RemoveStudentsFromCourseCommand.builder()
                .courseId(courseId)
                .studentEmails(List.of("student@example.com"))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(anotherTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        assertThrows(IllegalArgumentException.class,
                () -> removeStudentsFromCourseService.removeStudentsFromCourse(command));

        verify(saveCourseEnrollmentPort, never()).removeStudentsFromCourse(any(UUID.class), any(List.class));
    }

    @Test
    @DisplayName("Should throw StudentsNotFoundException when some emails are not found in the system")
    void testRemoveStudentsFromCourse_StudentsNotFound() {
        String existingEmail = "exists@example.com";
        String missingEmail = "missing@example.com";

        Map<String, UUID> emailToIdMap = new HashMap<>();
        emailToIdMap.put(existingEmail, student1Id);

        RemoveStudentsFromCourseCommand command = RemoveStudentsFromCourseCommand.builder()
                .courseId(courseId)
                .studentEmails(List.of(existingEmail, missingEmail))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadStudentsByEmailPort.findStudentIdsByEmails(List.of(existingEmail, missingEmail))).thenReturn(emailToIdMap);

        StudentsNotFoundException ex = assertThrows(StudentsNotFoundException.class,
                () -> removeStudentsFromCourseService.removeStudentsFromCourse(command));

        assertTrue(ex.getMissingEmails().contains(missingEmail));
        assertEquals(1, ex.getMissingEmails().size());
        verify(saveCourseEnrollmentPort, never()).removeStudentsFromCourse(any(UUID.class), any(List.class));
    }

    @Test
    @DisplayName("Should throw StudentNotEnrolledInCourseException when students are not enrolled")
    void testRemoveStudentsFromCourse_StudentsNotEnrolled() {
        UUID notEnrolledStudentId = UUID.randomUUID();
        String enrolledEmail = "student1@example.com";
        String notEnrolledEmail = "notenrolled@example.com";

        Map<String, UUID> emailToIdMap = new HashMap<>();
        emailToIdMap.put(enrolledEmail, student1Id);
        emailToIdMap.put(notEnrolledEmail, notEnrolledStudentId);

        RemoveStudentsFromCourseCommand command = RemoveStudentsFromCourseCommand.builder()
                .courseId(courseId)
                .studentEmails(List.of(enrolledEmail, notEnrolledEmail))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadStudentsByEmailPort.findStudentIdsByEmails(List.of(enrolledEmail, notEnrolledEmail))).thenReturn(emailToIdMap);

        StudentNotEnrolledInCourseException ex = assertThrows(StudentNotEnrolledInCourseException.class,
                () -> removeStudentsFromCourseService.removeStudentsFromCourse(command));

        assertTrue(ex.getNotEnrolledEmails().contains(notEnrolledEmail));
        assertEquals(1, ex.getNotEnrolledEmails().size());
        verify(saveCourseEnrollmentPort, never()).removeStudentsFromCourse(any(UUID.class), any(List.class));
    }
}
