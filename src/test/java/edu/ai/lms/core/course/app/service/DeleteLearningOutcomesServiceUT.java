package edu.ai.lms.core.course.app.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.User;
import edu.ai.lms.core.course.adapter.in.response.DeleteLearningOutcomesResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.DeleteLearningOutcomesCommand;
import edu.ai.lms.core.course.app.port.out.DeleteLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;

@ExtendWith(MockitoExtension.class)
class DeleteLearningOutcomesServiceUT {

    @Mock
    private DeleteLearningOutcomePort deleteLearningOutcomePort;

    @Mock
    private LoadLearningOutcomePort loadLearningOutcomePort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private DeleteLearningOutcomesService deleteLearningOutcomesService;

    private User mockUser;
    private UUID teacherId;
    private UUID courseId;
    private UUID loId1;
    private UUID loId2;
    private Course mockCourse;
    private LearningOutcome existingLo1;
    private LearningOutcome existingLo2;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        loId1 = UUID.randomUUID();
        loId2 = UUID.randomUUID();

        mockUser = User.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        mockCourse = Course.builder()
                .id(courseId)
                .courseName("Introduction to Java")
                .description("Learn Java programming basics")
                .courseCode("CS101")
                .active(true)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .teacherIds(List.of(teacherId))
                .build();

        existingLo1 = LearningOutcome.builder()
                .id(loId1).code("CLO-1").description("Description 1")
                .courseId(courseId).createdAt(LocalDateTime.now().minusDays(1)).build();

        existingLo2 = LearningOutcome.builder()
                .id(loId2).code("CLO-2").description("Description 2")
                .courseId(courseId).createdAt(LocalDateTime.now().minusDays(1)).build();
    }

    @Test
    @DisplayName("Should successfully delete multiple learning outcomes")
    void testDeleteLearningOutcomes_WithValidDetails_ShouldSucceed() {
        // Arrange
        DeleteLearningOutcomesCommand command = DeleteLearningOutcomesCommand.builder()
                .courseId(courseId)
                .learningOutcomeIds(List.of(loId1, loId2))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadLearningOutcomePort.findById(loId1)).thenReturn(Optional.of(existingLo1));
        when(loadLearningOutcomePort.findById(loId2)).thenReturn(Optional.of(existingLo2));

        // Act
        DeleteLearningOutcomesResponse response = deleteLearningOutcomesService.deleteLearningOutcomes(command);

        // Assert
        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals(2, response.getTotalDeleted());
        assertEquals(2, response.getDeletedIds().size());
        assertTrue(response.getDeletedIds().contains(loId1));
        assertTrue(response.getDeletedIds().contains(loId2));
        assertEquals("Learning outcomes deleted successfully", response.getMessage());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadLearningOutcomePort, times(2)).findById(any());
        verify(deleteLearningOutcomePort, times(1)).deleteAllByIds(List.of(loId1, loId2));
    }

    @Test
    @DisplayName("Should successfully delete a single learning outcome")
    void testDeleteLearningOutcomes_WithSingleItem_ShouldSucceed() {
        // Arrange
        DeleteLearningOutcomesCommand command = DeleteLearningOutcomesCommand.builder()
                .courseId(courseId)
                .learningOutcomeIds(List.of(loId1))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadLearningOutcomePort.findById(loId1)).thenReturn(Optional.of(existingLo1));

        // Act
        DeleteLearningOutcomesResponse response = deleteLearningOutcomesService.deleteLearningOutcomes(command);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalDeleted());

        verify(deleteLearningOutcomePort, times(1)).deleteAllByIds(List.of(loId1));
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher is not authenticated")
    void testDeleteLearningOutcomes_WithNonAuthenticatedTeacher_ShouldThrowException() {
        // Arrange
        DeleteLearningOutcomesCommand command = DeleteLearningOutcomesCommand.builder()
                .courseId(courseId).learningOutcomeIds(List.of(loId1)).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        TeacherNotFoundException exception = assertThrows(TeacherNotFoundException.class,
                () -> deleteLearningOutcomesService.deleteLearningOutcomes(command));

        assertEquals("Teacher not authenticated", exception.getMessage());

        verify(loadCoursePort, never()).findById(any());
        verify(loadLearningOutcomePort, never()).findById(any());
        verify(deleteLearningOutcomePort, never()).deleteAllByIds(anyList());
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testDeleteLearningOutcomes_WithNonExistentCourse_ShouldThrowException() {
        // Arrange
        DeleteLearningOutcomesCommand command = DeleteLearningOutcomesCommand.builder()
                .courseId(courseId).learningOutcomeIds(List.of(loId1)).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        // Act & Assert
        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class,
                () -> deleteLearningOutcomesService.deleteLearningOutcomes(command));

        assertTrue(exception.getMessage().contains("Course not found with id:"));

        verify(loadLearningOutcomePort, never()).findById(any());
        verify(deleteLearningOutcomePort, never()).deleteAllByIds(anyList());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher does not own the course")
    void testDeleteLearningOutcomes_WithUnauthorizedTeacher_ShouldThrowException() {
        // Arrange
        UUID anotherTeacherId = UUID.randomUUID();
        User anotherUser = User.builder().id(anotherTeacherId).role(Roles.TEACHER).build();

        DeleteLearningOutcomesCommand command = DeleteLearningOutcomesCommand.builder()
                .courseId(courseId).learningOutcomeIds(List.of(loId1)).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(anotherUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> deleteLearningOutcomesService.deleteLearningOutcomes(command));

        assertEquals("Teacher is not authorized to delete learning outcomes in this course", exception.getMessage());

        verify(loadLearningOutcomePort, never()).findById(any());
        verify(deleteLearningOutcomePort, never()).deleteAllByIds(anyList());
    }

    @Test
    @DisplayName("Should throw LearningOutcomeNotFoundException when any learning outcome does not exist")
    void testDeleteLearningOutcomes_WithNonExistentLo_ShouldThrowException() {
        // Arrange
        DeleteLearningOutcomesCommand command = DeleteLearningOutcomesCommand.builder()
                .courseId(courseId).learningOutcomeIds(List.of(loId1)).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadLearningOutcomePort.findById(loId1)).thenReturn(Optional.empty());

        // Act & Assert
        LearningOutcomeNotFoundException exception = assertThrows(LearningOutcomeNotFoundException.class,
                () -> deleteLearningOutcomesService.deleteLearningOutcomes(command));

        assertTrue(exception.getMessage().contains("Learning outcome not found with id:"));

        verify(deleteLearningOutcomePort, never()).deleteAllByIds(anyList());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when a learning outcome does not belong to the course")
    void testDeleteLearningOutcomes_WithLoBelongingToDifferentCourse_ShouldThrowException() {
        // Arrange
        UUID differentCourseId = UUID.randomUUID();
        LearningOutcome loFromDifferentCourse = LearningOutcome.builder()
                .id(loId1).code("CLO-1").description("Desc").courseId(differentCourseId).build();

        DeleteLearningOutcomesCommand command = DeleteLearningOutcomesCommand.builder()
                .courseId(courseId).learningOutcomeIds(List.of(loId1)).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadLearningOutcomePort.findById(loId1)).thenReturn(Optional.of(loFromDifferentCourse));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> deleteLearningOutcomesService.deleteLearningOutcomes(command));

        assertTrue(exception.getMessage().contains("does not belong to this course"));

        verify(deleteLearningOutcomePort, never()).deleteAllByIds(anyList());
    }
}
