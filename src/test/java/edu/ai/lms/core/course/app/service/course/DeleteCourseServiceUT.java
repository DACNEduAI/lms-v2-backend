package edu.ai.lms.core.course.app.service.course;

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
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.DeleteCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.out.DeleteCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.CleanupCourseMaterialsPort;

@ExtendWith(MockitoExtension.class)
class DeleteCourseServiceUT {

    @Mock
    private DeleteCoursePort deleteCoursePort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private CleanupCourseMaterialsPort cleanupCourseMaterialsPort;

    @InjectMocks
    private DeleteCourseService deleteCourseService;

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
                .courseName("Introduction to Java")
                .courseCode("CS101")
                .description("Learn Java programming basics")
                .teacherIds(List.of(teacherId))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully delete course with valid details")
    void testDeleteCourse_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        doNothing().when(cleanupCourseMaterialsPort).deleteAllByCourseId(courseId);
        doNothing().when(deleteCoursePort).deleteById(courseId);

        // Act
        DeleteCourseResponse response = deleteCourseService.deleteCourse(courseId);

        // Assert
        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals("Course deleted successfully", response.getMessage());
        assertTrue(response.getSuccess());
        
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(cleanupCourseMaterialsPort, times(1)).deleteAllByCourseId(courseId);
        verify(deleteCoursePort, times(1)).deleteById(courseId);
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testDeleteCourse_WithNonExistentCourse_ShouldThrowException() {
        // Arrange
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        // Act & Assert
        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class,
                () -> deleteCourseService.deleteCourse(courseId));

        assertEquals("Course not found with id: " + courseId, exception.getMessage());
        
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadSignedInUserPort, never()).findAuthenticatedUser();
        verify(cleanupCourseMaterialsPort, never()).deleteAllByCourseId(any());
        verify(deleteCoursePort, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should throw exception when teacher is not authorized to delete course")
    void testDeleteCourse_WithUnauthorizedTeacher_ShouldThrowException() {
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
                () -> deleteCourseService.deleteCourse(courseId));

        assertEquals("Teacher is not authorized to delete this course", exception.getMessage());
        
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(cleanupCourseMaterialsPort, never()).deleteAllByCourseId(any());
        verify(deleteCoursePort, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should call deleteById with correct courseId")
    void testDeleteCourse_ShouldCallDeleteByIdWithCorrectCourseId() {
        // Arrange
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        doNothing().when(cleanupCourseMaterialsPort).deleteAllByCourseId(courseId);
        doNothing().when(deleteCoursePort).deleteById(courseId);

        // Act
        deleteCourseService.deleteCourse(courseId);

        // Assert
        verify(cleanupCourseMaterialsPort, times(1)).deleteAllByCourseId(courseId);
        verify(deleteCoursePort, times(1)).deleteById(courseId);
    }

    @Test
    @DisplayName("Should return success response after successful deletion")
    void testDeleteCourse_ShouldReturnSuccessResponse() {
        // Arrange
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        doNothing().when(cleanupCourseMaterialsPort).deleteAllByCourseId(courseId);
        doNothing().when(deleteCoursePort).deleteById(courseId);

        // Act
        DeleteCourseResponse response = deleteCourseService.deleteCourse(courseId);

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals(courseId, response.getCourseId());
        assertNotNull(response.getMessage());
    }

    @Test
    @DisplayName("Should verify course ownership before deletion")
    void testDeleteCourse_ShouldVerifyCourseOwnership() {
        // Arrange
        CurrentActor courseOwner = CurrentActor.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();
        
        Course courseWithOwner = Course.builder()
                .id(courseId)
                .courseName("Test Course")
                .teacherIds(List.of(teacherId))
                .build();
        
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(courseWithOwner));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(courseOwner));
        doNothing().when(cleanupCourseMaterialsPort).deleteAllByCourseId(courseId);
        doNothing().when(deleteCoursePort).deleteById(courseId);

        // Act
        DeleteCourseResponse response = deleteCourseService.deleteCourse(courseId);

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(cleanupCourseMaterialsPort, times(1)).deleteAllByCourseId(courseId);
        verify(deleteCoursePort, times(1)).deleteById(courseId);
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when authenticated teacher not found")
    void testDeleteCourse_WithNoAuthenticatedTeacher_ShouldThrowException() {
        // Arrange
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        TeacherNotFoundException exception = assertThrows(TeacherNotFoundException.class,
                () -> deleteCourseService.deleteCourse(courseId));

        assertEquals("Authenticated teacher not found", exception.getMessage());
        
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(cleanupCourseMaterialsPort, never()).deleteAllByCourseId(any());
        verify(deleteCoursePort, never()).deleteById(any());
    }
}
