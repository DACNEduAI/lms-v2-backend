package edu.ai.lms.core.course.app.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

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

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.ModifyCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.ModifyCourseCommand;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;

@ExtendWith(MockitoExtension.class)
class ModifyCourseServiceUT {

    @Mock
    private SaveCoursePort saveCoursePort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ModifyCourseService modifyCourseService;

    private ModifyCourseCommand validCommand;
    private CurrentActor mockUser;
    private Course existingCourse;
    private UUID teacherId;
    private UUID courseId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        mockUser = CurrentActor.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        existingCourse = Course.builder()
                .id(courseId)
                .courseName("Old Course Name")
                .courseCode("CS101")
                .description("Old description")
                .teacherIds(List.of(teacherId))
                .active(true)
                .build();

        Course course = Course.builder()
                .courseName("Updated Course Name")
                .description("Updated description")
                .courseCode("CS101")
                .active(true)
                .coverImage("https://example.com/new-image.jpg")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .build();

        validCommand = ModifyCourseCommand.builder()
                .courseId(courseId)
                .course(course)
                .build();
    }

    @Test
    @DisplayName("Should successfully modify course with valid details")
    void testModifyCourse_WithValidDetails_ShouldSucceed() {
        // Arrange
        Course course = validCommand.course();
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(course.getCourseName())
                .courseCode(course.getCourseCode())
                .description(course.getDescription())
                .teacherIds(List.of(teacherId))
                .active(true)
                .coverImage(course.getCoverImage())
                .startDate(course.getStartDate())
                .endDate(course.getEndDate())
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        ModifyCourseResponse response = modifyCourseService.modifyCourse(validCommand);

        // Assert
        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals("Updated Course Name", response.getCourseName());
        assertEquals("CS101", response.getCourseCode());
        assertEquals("Updated description", response.getDescription());
        
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(saveCoursePort, times(1)).save(any(Course.class));
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testModifyCourse_WithNonExistentCourse_ShouldThrowException() {
        // Arrange
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        // Act & Assert
        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class,
                () -> modifyCourseService.modifyCourse(validCommand));

        assertEquals("Course not found with id: " + courseId, exception.getMessage());
        
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadSignedInUserPort, never()).findAuthenticatedUser();
        verify(saveCoursePort, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher does not exist")
    void testModifyCourse_WithNonExistentTeacher_ShouldThrowException() {
        // Arrange
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        TeacherNotFoundException exception = assertThrows(TeacherNotFoundException.class,
                () -> modifyCourseService.modifyCourse(validCommand));

        assertEquals("Authenticated teacher not found", exception.getMessage());
        
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(saveCoursePort, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Should throw exception when teacher is not authorized to modify course")
    void testModifyCourse_WithUnauthorizedTeacher_ShouldThrowException() {
        // Arrange
        UUID differentTeacherId = UUID.randomUUID();
        CurrentActor differentUser = CurrentActor.builder()
                .id(differentTeacherId)
                .role(Roles.TEACHER)
                .build();
        
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(differentUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> modifyCourseService.modifyCourse(validCommand));

        assertEquals("Teacher is not authorized to modify this course", exception.getMessage());
        
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(saveCoursePort, never()).save(any(Course.class));
    }

    @Test
        @DisplayName("Should save course with updated details")
    void testModifyCourse_ShouldSaveCourseWithUpdatedDetails() {
        // Arrange
        Course course = validCommand.course();
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(course.getCourseName())
                .courseCode(course.getCourseCode())
                .teacherIds(List.of(teacherId))
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        modifyCourseService.modifyCourse(validCommand);

        // Assert
        verify(saveCoursePort, times(1)).save(courseCaptor.capture());
        Course capturedCourse = courseCaptor.getValue();
        
        assertEquals(courseId, capturedCourse.getId());
        assertEquals("Updated Course Name", capturedCourse.getCourseName());
        assertEquals("Updated description", capturedCourse.getDescription());
        assertTrue(capturedCourse.getTeacherIds().contains(teacherId));
    }

    @Test
    @DisplayName("Should return response with updatedAt timestamp")
    void testModifyCourse_ShouldReturnResponseWithUpdatedAt() {
        // Arrange
        Course course = validCommand.course();
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(course.getCourseName())
                .courseCode(course.getCourseCode())
                .teacherIds(List.of(teacherId))
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        ModifyCourseResponse response = modifyCourseService.modifyCourse(validCommand);

        // Assert
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle modify course without learning outcomes payload")
    void testModifyCourse_WithoutLearningOutcomes_ShouldSucceed() {
        // Arrange
        Course noLearningOutcomesCourse = Course.builder()
                .courseName("Updated Course Name")
                .description("Updated description")
                .courseCode("CS101")
                .active(true)
                .coverImage("https://example.com/new-image.jpg")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .build();
        ModifyCourseCommand noLearningOutcomesCommand = ModifyCourseCommand.builder()
                .courseId(courseId)
                .course(noLearningOutcomesCourse)
                .build();
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(noLearningOutcomesCourse.getCourseName())
                .courseCode(noLearningOutcomesCourse.getCourseCode())
                .teacherIds(List.of(teacherId))
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        ModifyCourseResponse response = modifyCourseService.modifyCourse(noLearningOutcomesCommand);

        // Assert
        assertNotNull(response);
        verify(saveCoursePort, times(1)).save(any(Course.class));
    }
}
