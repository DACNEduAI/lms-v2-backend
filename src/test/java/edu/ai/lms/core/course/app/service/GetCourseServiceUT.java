package edu.ai.lms.core.course.app.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import edu.ai.lms.core.auth.app.domain.User;
import edu.ai.lms.core.course.adapter.in.response.GetCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;

@ExtendWith(MockitoExtension.class)
class GetCourseServiceUT {

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private LoadGroupLearningOutcomePort loadGroupLearningOutcomePort;

    @InjectMocks
    private GetCourseService getCourseService;

    private User mockUser;
    private UUID teacherId;
    private UUID courseId;
    private Course mockCourse;
    private UUID groupId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        groupId = UUID.randomUUID();

        mockUser = User.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        List<LearningOutcome> learningOutcomes = new ArrayList<>();
        learningOutcomes.add(LearningOutcome.builder()
                .id(UUID.randomUUID())
                .code("CLO-1")
                .description("Understand basic concepts")
                .groupLearningOutcomeId(groupId)
                .build());
        learningOutcomes.add(LearningOutcome.builder()
                .id(UUID.randomUUID())
                .code("CLO-2")
                .description("Apply advanced techniques")
                .build());

        mockCourse = Course.builder()
                .id(courseId)
                .courseName("Introduction to Java")
                .description("Learn Java programming basics")
                .courseCode("CS101")
                .active(true)
                .coverImage("https://example.com/image.jpg")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .teacherIds(List.of(teacherId))
                .learningOutcomes(learningOutcomes)
                .build();
    }

    @Test
    @DisplayName("Should successfully get a course with grouped and ungrouped learning outcomes")
    void testGetCourse_WithValidCourseIdAndAuthenticatedTeacher_ShouldSucceed() {
        // Arrange
        GroupLearningOutcome group = GroupLearningOutcome.builder()
                .id(groupId)
                .name("Fundamentals")
                .description("Basic concepts group")
                .courseId(courseId)
                .learningOutcomes(List.of(mockCourse.getLearningOutcomes().get(0)))
                .build();

        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadGroupLearningOutcomePort.findByCourseId(courseId)).thenReturn(List.of(group));

        // Act
        GetCourseResponse response = getCourseService.getCourse(courseId);

        // Assert
        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals("Introduction to Java", response.getCourseName());
        assertEquals("CS101", response.getCourseCode());
        assertEquals(teacherId, response.getTeacherId());
        assertTrue(response.getActive());

        // Grouped LOs
        assertNotNull(response.getGroupLearningOutcomes());
        assertEquals(1, response.getGroupLearningOutcomes().size());
        assertEquals("Fundamentals", response.getGroupLearningOutcomes().get(0).getName());
        assertEquals(1, response.getGroupLearningOutcomes().get(0).getLearningOutcomes().size());
        assertEquals("CLO-1", response.getGroupLearningOutcomes().get(0).getLearningOutcomes().get(0).getCode());

        // Ungrouped LOs
        assertNotNull(response.getUngroupedLearningOutcomes());
        assertEquals(1, response.getUngroupedLearningOutcomes().size());
        assertEquals("CLO-2", response.getUngroupedLearningOutcomes().get(0).getCode());

        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadGroupLearningOutcomePort, times(1)).findByCourseId(courseId);
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testGetCourse_WithNonExistentCourse_ShouldThrowException() {
        // Arrange
        UUID nonExistentCourseId = UUID.randomUUID();
        when(loadCoursePort.findById(nonExistentCourseId)).thenReturn(Optional.empty());

        // Act & Assert
        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class,
                () -> getCourseService.getCourse(nonExistentCourseId));

        assertTrue(exception.getMessage().contains("Course not found with id:"));

        verify(loadCoursePort, times(1)).findById(nonExistentCourseId);
        verify(loadSignedInUserPort, never()).findAuthenticatedUser();
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher is not authenticated")
    void testGetCourse_WithNonAuthenticatedTeacher_ShouldThrowException() {
        // Arrange
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        TeacherNotFoundException exception = assertThrows(TeacherNotFoundException.class,
                () -> getCourseService.getCourse(courseId));

        assertEquals("Authenticated teacher not found", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher is not authorized to access the course")
    void testGetCourse_WithUnauthorizedTeacher_ShouldThrowException() {
        // Arrange
        User anotherUser = User.builder()
                .id(UUID.randomUUID())
                .role(Roles.TEACHER)
                .build();

        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(anotherUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> getCourseService.getCourse(courseId));

        assertEquals("Teacher is not authorized to access this course", exception.getMessage());
    }

    @Test
    @DisplayName("Should successfully get a course with empty learning outcomes")
    void testGetCourse_WithEmptyLearningOutcomes_ShouldSucceed() {
        // Arrange
        Course courseWithNoLearningOutcomes = Course.builder()
                .id(courseId)
                .courseName("Empty Course")
                .description("A course with no learning outcomes")
                .courseCode("CS102")
                .active(true)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .teacherIds(List.of(teacherId))
                .learningOutcomes(new ArrayList<>())
                .build();

        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(courseWithNoLearningOutcomes));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadGroupLearningOutcomePort.findByCourseId(courseId)).thenReturn(Collections.emptyList());

        // Act
        GetCourseResponse response = getCourseService.getCourse(courseId);

        // Assert
        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertNotNull(response.getGroupLearningOutcomes());
        assertEquals(0, response.getGroupLearningOutcomes().size());
        assertNotNull(response.getUngroupedLearningOutcomes());
        assertEquals(0, response.getUngroupedLearningOutcomes().size());
    }

    @Test
    @DisplayName("Should successfully get an inactive course")
    void testGetCourse_WithInactiveCourse_ShouldSucceed() {
        // Arrange
        Course inactiveCourse = Course.builder()
                .id(courseId)
                .courseName("Inactive Course")
                .description("An inactive course")
                .courseCode("CS103")
                .active(false)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .teacherIds(List.of(teacherId))
                .learningOutcomes(new ArrayList<>())
                .build();

        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(inactiveCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadGroupLearningOutcomePort.findByCourseId(courseId)).thenReturn(Collections.emptyList());

        // Act
        GetCourseResponse response = getCourseService.getCourse(courseId);

        // Assert
        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertFalse(response.getActive());
    }
}
