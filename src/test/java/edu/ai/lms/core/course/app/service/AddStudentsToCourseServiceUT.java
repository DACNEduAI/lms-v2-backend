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
import edu.ai.lms.core.auth.app.domain.User;
import edu.ai.lms.core.course.adapter.in.response.AddStudentsToCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.StudentsNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.AddStudentsToCourseCommand;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.LoadStudentsByEmailPort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;

@ExtendWith(MockitoExtension.class)
class AddStudentsToCourseServiceUT {

    @Mock
    private SaveCoursePort saveCoursePort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private LoadStudentsByEmailPort loadStudentsByEmailPort;

    @InjectMocks
    private AddStudentsToCourseService addStudentsToCourseService;

    private UUID teacherId;
    private UUID courseId;
    private User mockTeacher;
    private Course mockCourse;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        mockTeacher = User.builder()
                .id(teacherId)
                .email("teacher@example.com")
                .role(Roles.TEACHER)
                .build();

        mockCourse = Course.builder()
                .id(courseId)
                .courseName("Test Course")
                .courseCode("TC101")
                .teacherIds(new ArrayList<>(List.of(teacherId)))
                .studentIds(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should successfully add students to a course")
    void testAddStudentsToCourse_Success() {
        UUID student1Id = UUID.randomUUID();
        UUID student2Id = UUID.randomUUID();
        String email1 = "student1@example.com";
        String email2 = "student2@example.com";

        Map<String, UUID> emailToIdMap = new HashMap<>();
        emailToIdMap.put(email1, student1Id);
        emailToIdMap.put(email2, student2Id);

        AddStudentsToCourseCommand command = AddStudentsToCourseCommand.builder()
                .courseId(courseId)
                .studentEmails(List.of(email1, email2))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadStudentsByEmailPort.findStudentIdsByEmails(List.of(email1, email2))).thenReturn(emailToIdMap);
        when(saveCoursePort.save(any(Course.class))).thenReturn(mockCourse);

        AddStudentsToCourseResponse response = addStudentsToCourseService.addStudentsToCourse(command);

        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals(2, response.getTotalAdded());
        assertEquals(2, response.getAddedStudentEmails().size());
        assertTrue(response.getAddedStudentEmails().contains(email1));
        assertTrue(response.getAddedStudentEmails().contains(email2));
        verify(saveCoursePort).save(any(Course.class));
    }

    @Test
    @DisplayName("Should skip duplicate students already in course")
    void testAddStudentsToCourse_SkipDuplicates() {
        UUID existingStudentId = UUID.randomUUID();
        UUID newStudentId = UUID.randomUUID();
        String existingEmail = "existing@example.com";
        String newEmail = "new@example.com";

        mockCourse.setStudentIds(new ArrayList<>(List.of(existingStudentId)));

        Map<String, UUID> emailToIdMap = new HashMap<>();
        emailToIdMap.put(existingEmail, existingStudentId);
        emailToIdMap.put(newEmail, newStudentId);

        AddStudentsToCourseCommand command = AddStudentsToCourseCommand.builder()
                .courseId(courseId)
                .studentEmails(List.of(existingEmail, newEmail))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadStudentsByEmailPort.findStudentIdsByEmails(List.of(existingEmail, newEmail))).thenReturn(emailToIdMap);
        when(saveCoursePort.save(any(Course.class))).thenReturn(mockCourse);

        AddStudentsToCourseResponse response = addStudentsToCourseService.addStudentsToCourse(command);

        assertNotNull(response);
        assertEquals(1, response.getTotalAdded());
        assertEquals(1, response.getAddedStudentEmails().size());
        assertTrue(response.getAddedStudentEmails().contains(newEmail));
        verify(saveCoursePort).save(any(Course.class));
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher is not authenticated")
    void testAddStudentsToCourse_TeacherNotAuthenticated() {
        AddStudentsToCourseCommand command = AddStudentsToCourseCommand.builder()
                .courseId(courseId)
                .studentEmails(List.of("student@example.com"))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        assertThrows(TeacherNotFoundException.class,
                () -> addStudentsToCourseService.addStudentsToCourse(command));

        verify(saveCoursePort, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testAddStudentsToCourse_CourseNotFound() {
        AddStudentsToCourseCommand command = AddStudentsToCourseCommand.builder()
                .courseId(courseId)
                .studentEmails(List.of("student@example.com"))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class,
                () -> addStudentsToCourseService.addStudentsToCourse(command));

        verify(saveCoursePort, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher does not own the course")
    void testAddStudentsToCourse_UnauthorizedTeacher() {
        UUID anotherTeacherId = UUID.randomUUID();
        User anotherTeacher = User.builder()
                .id(anotherTeacherId)
                .email("other@example.com")
                .role(Roles.TEACHER)
                .build();

        AddStudentsToCourseCommand command = AddStudentsToCourseCommand.builder()
                .courseId(courseId)
                .studentEmails(List.of("student@example.com"))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(anotherTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        assertThrows(IllegalArgumentException.class,
                () -> addStudentsToCourseService.addStudentsToCourse(command));

        verify(saveCoursePort, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Should throw StudentsNotFoundException when some emails are not found in the system")
    void testAddStudentsToCourse_StudentsNotFound() {
        String existingEmail = "exists@example.com";
        String missingEmail = "missing@example.com";
        UUID existingStudentId = UUID.randomUUID();

        Map<String, UUID> emailToIdMap = new HashMap<>();
        emailToIdMap.put(existingEmail, existingStudentId);
        // missingEmail is NOT in the map

        AddStudentsToCourseCommand command = AddStudentsToCourseCommand.builder()
                .courseId(courseId)
                .studentEmails(List.of(existingEmail, missingEmail))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadStudentsByEmailPort.findStudentIdsByEmails(List.of(existingEmail, missingEmail))).thenReturn(emailToIdMap);

        StudentsNotFoundException ex = assertThrows(StudentsNotFoundException.class,
                () -> addStudentsToCourseService.addStudentsToCourse(command));

        assertTrue(ex.getMissingEmails().contains(missingEmail));
        assertEquals(1, ex.getMissingEmails().size());
        verify(saveCoursePort, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Should successfully add a single student to the course")
    void testAddStudentsToCourse_SingleStudent() {
        UUID studentId = UUID.randomUUID();
        String email = "single@example.com";

        Map<String, UUID> emailToIdMap = new HashMap<>();
        emailToIdMap.put(email, studentId);

        AddStudentsToCourseCommand command = AddStudentsToCourseCommand.builder()
                .courseId(courseId)
                .studentEmails(List.of(email))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadStudentsByEmailPort.findStudentIdsByEmails(List.of(email))).thenReturn(emailToIdMap);
        when(saveCoursePort.save(any(Course.class))).thenReturn(mockCourse);

        AddStudentsToCourseResponse response = addStudentsToCourseService.addStudentsToCourse(command);

        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals(1, response.getTotalAdded());
        assertEquals("Students added to course successfully", response.getMessage());
    }
}
