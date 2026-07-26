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
import edu.ai.lms.core.course.adapter.in.response.ModifyLearningOutcomeResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.ModifySingleLearningOutcomeCommand;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveLearningOutcomePort;

@ExtendWith(MockitoExtension.class)
class ModifySingleLearningOutcomeServiceUT {

    @Mock
    private SaveLearningOutcomePort saveLearningOutcomePort;

    @Mock
    private LoadLearningOutcomePort loadLearningOutcomePort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private ModifySingleLearningOutcomeService modifySingleLearningOutcomeService;

    private CurrentActor mockUser;
    private UUID teacherId;
    private UUID courseId;
    private UUID learningOutcomeId;
    private Course mockCourse;
    private LearningOutcome existingLo;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        learningOutcomeId = UUID.randomUUID();

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

        existingLo = LearningOutcome.builder()
                .id(learningOutcomeId)
                .code("CLO-1")
                .description("Original description")
                .courseId(courseId)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    @DisplayName("Should successfully modify a single learning outcome")
    void testModifyLearningOutcome_WithValidDetails_ShouldSucceed() {
        // Arrange
        LearningOutcome updateData = LearningOutcome.builder()
                .code("CLO-1-UPDATED")
                .description("Updated description")
                .build();

        ModifySingleLearningOutcomeCommand command = ModifySingleLearningOutcomeCommand.builder()
                .courseId(courseId)
                .learningOutcomeId(learningOutcomeId)
                .learningOutcome(updateData)
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadLearningOutcomePort.findById(learningOutcomeId)).thenReturn(Optional.of(existingLo));

        LocalDateTime updatedAt = LocalDateTime.now();
        when(saveLearningOutcomePort.save(any(LearningOutcome.class)))
                .thenReturn(LearningOutcome.builder()
                        .id(learningOutcomeId)
                        .code("CLO-1-UPDATED")
                        .description("Updated description")
                        .courseId(courseId)
                        .createdAt(existingLo.getCreatedAt())
                        .updatedAt(updatedAt)
                        .build());

        // Act
        ModifyLearningOutcomeResponse response = modifySingleLearningOutcomeService.modifyLearningOutcome(command);

        // Assert
        assertNotNull(response);
        assertEquals(learningOutcomeId, response.getId());
        assertEquals("CLO-1-UPDATED", response.getCode());
        assertEquals("Updated description", response.getDescription());
        assertEquals(courseId, response.getCourseId());
        assertEquals(updatedAt, response.getUpdatedAt());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadLearningOutcomePort, times(1)).findById(learningOutcomeId);
        verify(saveLearningOutcomePort, times(1)).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher is not authenticated")
    void testModifyLearningOutcome_WithNonAuthenticatedTeacher_ShouldThrowException() {
        // Arrange
        LearningOutcome updateData = LearningOutcome.builder().code("CLO-1-UPDATED").description("Updated").build();
        ModifySingleLearningOutcomeCommand command = ModifySingleLearningOutcomeCommand.builder()
                .courseId(courseId).learningOutcomeId(learningOutcomeId).learningOutcome(updateData).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        TeacherNotFoundException exception = assertThrows(TeacherNotFoundException.class,
                () -> modifySingleLearningOutcomeService.modifyLearningOutcome(command));

        assertEquals("Teacher not authenticated", exception.getMessage());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, never()).findById(any());
        verify(loadLearningOutcomePort, never()).findById(any());
        verify(saveLearningOutcomePort, never()).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testModifyLearningOutcome_WithNonExistentCourse_ShouldThrowException() {
        // Arrange
        LearningOutcome updateData = LearningOutcome.builder().code("CLO-1-UPDATED").description("Updated").build();
        ModifySingleLearningOutcomeCommand command = ModifySingleLearningOutcomeCommand.builder()
                .courseId(courseId).learningOutcomeId(learningOutcomeId).learningOutcome(updateData).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        // Act & Assert
        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class,
                () -> modifySingleLearningOutcomeService.modifyLearningOutcome(command));

        assertTrue(exception.getMessage().contains("Course not found with id:"));

        verify(loadLearningOutcomePort, never()).findById(any());
        verify(saveLearningOutcomePort, never()).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher does not own the course")
    void testModifyLearningOutcome_WithUnauthorizedTeacher_ShouldThrowException() {
        // Arrange
        UUID anotherTeacherId = UUID.randomUUID();
        CurrentActor anotherUser = CurrentActor.builder().id(anotherTeacherId).role(Roles.TEACHER).build();

        LearningOutcome updateData = LearningOutcome.builder().code("CLO-1-UPDATED").description("Updated").build();
        ModifySingleLearningOutcomeCommand command = ModifySingleLearningOutcomeCommand.builder()
                .courseId(courseId).learningOutcomeId(learningOutcomeId).learningOutcome(updateData).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(anotherUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> modifySingleLearningOutcomeService.modifyLearningOutcome(command));

        assertEquals("Teacher is not authorized to modify learning outcomes in this course", exception.getMessage());

        verify(loadLearningOutcomePort, never()).findById(any());
        verify(saveLearningOutcomePort, never()).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should throw LearningOutcomeNotFoundException when learning outcome does not exist")
    void testModifyLearningOutcome_WithNonExistentLo_ShouldThrowException() {
        // Arrange
        LearningOutcome updateData = LearningOutcome.builder().code("CLO-1-UPDATED").description("Updated").build();
        ModifySingleLearningOutcomeCommand command = ModifySingleLearningOutcomeCommand.builder()
                .courseId(courseId).learningOutcomeId(learningOutcomeId).learningOutcome(updateData).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadLearningOutcomePort.findById(learningOutcomeId)).thenReturn(Optional.empty());

        // Act & Assert
        LearningOutcomeNotFoundException exception = assertThrows(LearningOutcomeNotFoundException.class,
                () -> modifySingleLearningOutcomeService.modifyLearningOutcome(command));

        assertTrue(exception.getMessage().contains("Learning outcome not found with id:"));

        verify(saveLearningOutcomePort, never()).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when learning outcome does not belong to the course")
    void testModifyLearningOutcome_WithLoBelongingToDifferentCourse_ShouldThrowException() {
        // Arrange
        UUID differentCourseId = UUID.randomUUID();
        LearningOutcome loFromDifferentCourse = LearningOutcome.builder()
                .id(learningOutcomeId)
                .code("CLO-1")
                .description("Original description")
                .courseId(differentCourseId)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        LearningOutcome updateData = LearningOutcome.builder().code("CLO-1-UPDATED").description("Updated").build();
        ModifySingleLearningOutcomeCommand command = ModifySingleLearningOutcomeCommand.builder()
                .courseId(courseId).learningOutcomeId(learningOutcomeId).learningOutcome(updateData).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadLearningOutcomePort.findById(learningOutcomeId)).thenReturn(Optional.of(loFromDifferentCourse));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> modifySingleLearningOutcomeService.modifyLearningOutcome(command));

        assertEquals("Learning outcome does not belong to this course", exception.getMessage());

        verify(saveLearningOutcomePort, never()).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should preserve existing id and courseId when modifying")
    void testModifyLearningOutcome_ShouldPreserveImmutableFields() {
        // Arrange
        LearningOutcome updateData = LearningOutcome.builder()
                .code("CLO-1-UPDATED")
                .description("Updated description")
                .build();

        ModifySingleLearningOutcomeCommand command = ModifySingleLearningOutcomeCommand.builder()
                .courseId(courseId).learningOutcomeId(learningOutcomeId).learningOutcome(updateData).build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadLearningOutcomePort.findById(learningOutcomeId)).thenReturn(Optional.of(existingLo));

        when(saveLearningOutcomePort.save(any(LearningOutcome.class)))
                .thenAnswer(invocation -> {
                    LearningOutcome toSave = invocation.getArgument(0);
                    // Verify immutable fields are preserved
                    assertEquals(learningOutcomeId, toSave.getId());
                    assertEquals(courseId, toSave.getCourseId());
                    assertEquals(existingLo.getCreatedAt(), toSave.getCreatedAt());
                    return LearningOutcome.builder()
                            .id(toSave.getId()).code(toSave.getCode()).description(toSave.getDescription())
                            .courseId(toSave.getCourseId()).createdAt(toSave.getCreatedAt())
                            .updatedAt(LocalDateTime.now()).build();
                });

        // Act
        ModifyLearningOutcomeResponse response = modifySingleLearningOutcomeService.modifyLearningOutcome(command);

        // Assert
        assertNotNull(response);
        assertEquals(learningOutcomeId, response.getId());
        assertEquals(courseId, response.getCourseId());

        verify(saveLearningOutcomePort, times(1)).save(any(LearningOutcome.class));
    }
}
