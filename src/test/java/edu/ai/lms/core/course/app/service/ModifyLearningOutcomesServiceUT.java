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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.ModifyLearningOutcomesResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.ModifyLearningOutcomesCommand;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveLearningOutcomePort;

@ExtendWith(MockitoExtension.class)
class ModifyLearningOutcomesServiceUT {

    @Mock
    private SaveLearningOutcomePort saveLearningOutcomePort;

    @Mock
    private LoadLearningOutcomePort loadLearningOutcomePort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private ModifyLearningOutcomesService modifyLearningOutcomesService;

    private CurrentActor mockUser;
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

        mockUser = CurrentActor.builder()
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
                .id(loId1).prefixNumber(1).sortOrder(1).description("Original description 1")
                .courseId(courseId).createdAt(LocalDateTime.now().minusDays(1)).build();

        existingLo2 = LearningOutcome.builder()
                .id(loId2).prefixNumber(2).sortOrder(2).description("Original description 2")
                .courseId(courseId).createdAt(LocalDateTime.now().minusDays(1)).build();
    }

    @Test
    @DisplayName("Should successfully modify multiple learning outcomes")
    void testModifyLearningOutcomes_WithValidDetails_ShouldSucceed() {
        // Arrange
        LearningOutcome update1 = LearningOutcome.builder().id(loId1).prefixNumber(11).sortOrder(11).description("Updated 1").build();
        LearningOutcome update2 = LearningOutcome.builder().id(loId2).prefixNumber(12).sortOrder(12).description("Updated 2").build();

        ModifyLearningOutcomesCommand command = ModifyLearningOutcomesCommand.builder()
                .courseId(courseId)
                .learningOutcomes(List.of(update1, update2))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadLearningOutcomePort.findById(loId1)).thenReturn(Optional.of(existingLo1));
        when(loadLearningOutcomePort.findById(loId2)).thenReturn(Optional.of(existingLo2));

        LocalDateTime updatedAt = LocalDateTime.now();
        when(saveLearningOutcomePort.save(any(LearningOutcome.class)))
                .thenAnswer(invocation -> {
                    LearningOutcome lo = invocation.getArgument(0);
                    return LearningOutcome.builder()
                            .id(lo.getId())
                            .prefixNumber(lo.getPrefixNumber())
                            .sortOrder(lo.getSortOrder())
                            .description(lo.getDescription())
                            .courseId(lo.getCourseId()).createdAt(lo.getCreatedAt()).updatedAt(updatedAt).build();
                });

        // Act
        ModifyLearningOutcomesResponse response = modifyLearningOutcomesService.modifyLearningOutcomes(command);

        // Assert
        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals(2, response.getTotalModified());
        assertEquals(2, response.getLearningOutcomes().size());
        assertEquals(11, response.getLearningOutcomes().get(0).getPrefixNumber());
        assertEquals(12, response.getLearningOutcomes().get(1).getPrefixNumber());

        verify(loadLearningOutcomePort, times(2)).findById(any());
        verify(saveLearningOutcomePort, times(2)).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher is not authenticated")
    void testModifyLearningOutcomes_WithNonAuthenticatedTeacher_ShouldThrowException() {
        // Arrange
        LearningOutcome update = LearningOutcome.builder().id(loId1).code("CLO-1-UPD").description("Updated").build();
        ModifyLearningOutcomesCommand command = ModifyLearningOutcomesCommand.builder()
                .courseId(courseId).learningOutcomes(List.of(update)).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        TeacherNotFoundException exception = assertThrows(TeacherNotFoundException.class,
                () -> modifyLearningOutcomesService.modifyLearningOutcomes(command));

        assertEquals("Teacher not authenticated", exception.getMessage());

        verify(loadCoursePort, never()).findById(any());
        verify(loadLearningOutcomePort, never()).findById(any());
        verify(saveLearningOutcomePort, never()).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testModifyLearningOutcomes_WithNonExistentCourse_ShouldThrowException() {
        // Arrange
        LearningOutcome update = LearningOutcome.builder().id(loId1).code("CLO-1-UPD").description("Updated").build();
        ModifyLearningOutcomesCommand command = ModifyLearningOutcomesCommand.builder()
                .courseId(courseId).learningOutcomes(List.of(update)).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        // Act & Assert
        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class,
                () -> modifyLearningOutcomesService.modifyLearningOutcomes(command));

        assertTrue(exception.getMessage().contains("Course not found with id:"));

        verify(loadLearningOutcomePort, never()).findById(any());
        verify(saveLearningOutcomePort, never()).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher does not own the course")
    void testModifyLearningOutcomes_WithUnauthorizedTeacher_ShouldThrowException() {
        // Arrange
        UUID anotherTeacherId = UUID.randomUUID();
        CurrentActor anotherUser = CurrentActor.builder().id(anotherTeacherId).role(Roles.TEACHER).build();

        LearningOutcome update = LearningOutcome.builder().id(loId1).code("CLO-1-UPD").description("Updated").build();
        ModifyLearningOutcomesCommand command = ModifyLearningOutcomesCommand.builder()
                .courseId(courseId).learningOutcomes(List.of(update)).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(anotherUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> modifyLearningOutcomesService.modifyLearningOutcomes(command));

        assertEquals("Teacher is not authorized to modify learning outcomes in this course", exception.getMessage());

        verify(loadLearningOutcomePort, never()).findById(any());
        verify(saveLearningOutcomePort, never()).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should throw LearningOutcomeNotFoundException when any learning outcome does not exist")
    void testModifyLearningOutcomes_WithNonExistentLo_ShouldThrowException() {
        // Arrange
        LearningOutcome update = LearningOutcome.builder().id(loId1).code("CLO-1-UPD").description("Updated").build();
        ModifyLearningOutcomesCommand command = ModifyLearningOutcomesCommand.builder()
                .courseId(courseId).learningOutcomes(List.of(update)).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadLearningOutcomePort.findById(loId1)).thenReturn(Optional.empty());

        // Act & Assert
        LearningOutcomeNotFoundException exception = assertThrows(LearningOutcomeNotFoundException.class,
                () -> modifyLearningOutcomesService.modifyLearningOutcomes(command));

        assertTrue(exception.getMessage().contains("Learning outcome not found with id:"));

        verify(saveLearningOutcomePort, never()).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when a learning outcome does not belong to the course")
    void testModifyLearningOutcomes_WithLoBelongingToDifferentCourse_ShouldThrowException() {
        // Arrange
        UUID differentCourseId = UUID.randomUUID();
        LearningOutcome loFromDifferentCourse = LearningOutcome.builder()
                .id(loId1).code("CLO-1").description("Orig").courseId(differentCourseId).build();

        LearningOutcome update = LearningOutcome.builder().id(loId1).code("CLO-1-UPD").description("Updated").build();
        ModifyLearningOutcomesCommand command = ModifyLearningOutcomesCommand.builder()
                .courseId(courseId).learningOutcomes(List.of(update)).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadLearningOutcomePort.findById(loId1)).thenReturn(Optional.of(loFromDifferentCourse));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> modifyLearningOutcomesService.modifyLearningOutcomes(command));

        assertTrue(exception.getMessage().contains("does not belong to this course"));

        verify(saveLearningOutcomePort, never()).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should successfully modify a single learning outcome in batch")
    void testModifyLearningOutcomes_WithSingleItem_ShouldSucceed() {
        // Arrange
        LearningOutcome update = LearningOutcome.builder().id(loId1).code("CLO-1-UPD").description("Updated").build();
        ModifyLearningOutcomesCommand command = ModifyLearningOutcomesCommand.builder()
                .courseId(courseId).learningOutcomes(List.of(update)).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadLearningOutcomePort.findById(loId1)).thenReturn(Optional.of(existingLo1));

        LocalDateTime updatedAt = LocalDateTime.now();
        when(saveLearningOutcomePort.save(any(LearningOutcome.class)))
                .thenReturn(LearningOutcome.builder()
                        .id(loId1).code("CLO-1-UPD").description("Updated")
                        .courseId(courseId).createdAt(existingLo1.getCreatedAt()).updatedAt(updatedAt).build());

        // Act
        ModifyLearningOutcomesResponse response = modifyLearningOutcomesService.modifyLearningOutcomes(command);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalModified());
        assertEquals(1, response.getLearningOutcomes().size());

        verify(saveLearningOutcomePort, times(1)).save(any(LearningOutcome.class));
    }
}
