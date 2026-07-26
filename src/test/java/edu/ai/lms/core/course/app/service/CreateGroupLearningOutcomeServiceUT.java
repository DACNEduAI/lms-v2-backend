package edu.ai.lms.core.course.app.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import edu.ai.lms.core.course.adapter.in.response.CreateGroupLearningOutcomeResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeAlreadyInGroupException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.CreateGroupLearningOutcomeCommand;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.SaveLearningOutcomePort;

@ExtendWith(MockitoExtension.class)
class CreateGroupLearningOutcomeServiceUT {

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadLearningOutcomePort loadLearningOutcomePort;

    @Mock
    private SaveGroupLearningOutcomePort saveGroupLearningOutcomePort;

    @Mock
    private SaveLearningOutcomePort saveLearningOutcomePort;

    @InjectMocks
    private CreateGroupLearningOutcomeService createGroupLearningOutcomeService;

    private CurrentActor mockUser;
    private UUID teacherId;
    private UUID courseId;
    private Course mockCourse;
    private UUID loId1;
    private UUID loId2;
    private LearningOutcome lo1;
    private LearningOutcome lo2;

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
                .courseName("Test Course")
                .courseCode("CS101")
                .active(true)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .teacherIds(List.of(teacherId))
                .learningOutcomes(new ArrayList<>())
                .build();

        lo1 = LearningOutcome.builder()
                .id(loId1)
                .code("CLO-1")
                .description("Understand basic concepts")
                .courseId(courseId)
                .build();

        lo2 = LearningOutcome.builder()
                .id(loId2)
                .code("CLO-2")
                .description("Apply advanced techniques")
                .courseId(courseId)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a group learning outcome with learning outcomes")
    void testCreateGroupLearningOutcome_Success() {
        // Arrange
        UUID savedGroupId = UUID.randomUUID();
        CreateGroupLearningOutcomeCommand command = CreateGroupLearningOutcomeCommand.builder()
                .courseId(courseId)
                .name("Fundamentals")
                .description("Basic concepts group")
                .learningOutcomeIds(List.of(loId1, loId2))
                .build();

        GroupLearningOutcome savedGroup = GroupLearningOutcome.builder()
                .id(savedGroupId)
                .name("Fundamentals")
                .description("Basic concepts group")
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadLearningOutcomePort.findById(loId1)).thenReturn(Optional.of(lo1));
        when(loadLearningOutcomePort.findById(loId2)).thenReturn(Optional.of(lo2));
        when(saveGroupLearningOutcomePort.save(any(GroupLearningOutcome.class))).thenReturn(savedGroup);

        // Act
        CreateGroupLearningOutcomeResponse response = createGroupLearningOutcomeService.createGroupLearningOutcome(command);

        // Assert
        assertNotNull(response);
        assertEquals(savedGroupId, response.getId());
        assertEquals("Fundamentals", response.getName());
        assertEquals("Basic concepts group", response.getDescription());
        assertEquals(courseId, response.getCourseId());
        assertEquals(2, response.getLearningOutcomes().size());
        assertEquals("CLO-1", response.getLearningOutcomes().get(0).getCode());
        assertEquals("CLO-2", response.getLearningOutcomes().get(1).getCode());

        verify(saveGroupLearningOutcomePort, times(1)).save(any(GroupLearningOutcome.class));
        verify(saveLearningOutcomePort, times(1)).assignToGroup(loId1, courseId, savedGroupId);
        verify(saveLearningOutcomePort, times(1)).assignToGroup(loId2, courseId, savedGroupId);
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher is not authenticated")
    void testCreateGroupLearningOutcome_TeacherNotAuthenticated() {
        // Arrange
        CreateGroupLearningOutcomeCommand command = CreateGroupLearningOutcomeCommand.builder()
                .courseId(courseId)
                .name("Group")
                .learningOutcomeIds(List.of(loId1))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TeacherNotFoundException.class,
                () -> createGroupLearningOutcomeService.createGroupLearningOutcome(command));

        verify(loadCoursePort, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testCreateGroupLearningOutcome_CourseNotFound() {
        // Arrange
        UUID nonExistentCourseId = UUID.randomUUID();
        CreateGroupLearningOutcomeCommand command = CreateGroupLearningOutcomeCommand.builder()
                .courseId(nonExistentCourseId)
                .name("Group")
                .learningOutcomeIds(List.of(loId1))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(nonExistentCourseId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CourseNotFoundException.class,
                () -> createGroupLearningOutcomeService.createGroupLearningOutcome(command));

        verify(loadLearningOutcomePort, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher is not authorized")
    void testCreateGroupLearningOutcome_TeacherNotAuthorized() {
        // Arrange
        CurrentActor anotherUser = CurrentActor.builder()
                .id(UUID.randomUUID())
                .role(Roles.TEACHER)
                .build();

        CreateGroupLearningOutcomeCommand command = CreateGroupLearningOutcomeCommand.builder()
                .courseId(courseId)
                .name("Group")
                .learningOutcomeIds(List.of(loId1))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(anotherUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createGroupLearningOutcomeService.createGroupLearningOutcome(command));

        assertEquals("Teacher is not authorized to modify this course", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw LearningOutcomeNotFoundException when learning outcome does not exist")
    void testCreateGroupLearningOutcome_LearningOutcomeNotFound() {
        // Arrange
        UUID nonExistentLoId = UUID.randomUUID();
        CreateGroupLearningOutcomeCommand command = CreateGroupLearningOutcomeCommand.builder()
                .courseId(courseId)
                .name("Group")
                .learningOutcomeIds(List.of(nonExistentLoId))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadLearningOutcomePort.findById(nonExistentLoId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(LearningOutcomeNotFoundException.class,
                () -> createGroupLearningOutcomeService.createGroupLearningOutcome(command));

        verify(saveGroupLearningOutcomePort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when learning outcome does not belong to course")
    void testCreateGroupLearningOutcome_LearningOutcomeNotInCourse() {
        // Arrange
        UUID otherCourseId = UUID.randomUUID();
        LearningOutcome loFromOtherCourse = LearningOutcome.builder()
                .id(loId1)
                .code("CLO-1")
                .courseId(otherCourseId)
                .build();

        CreateGroupLearningOutcomeCommand command = CreateGroupLearningOutcomeCommand.builder()
                .courseId(courseId)
                .name("Group")
                .learningOutcomeIds(List.of(loId1))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadLearningOutcomePort.findById(loId1)).thenReturn(Optional.of(loFromOtherCourse));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createGroupLearningOutcomeService.createGroupLearningOutcome(command));

        assertTrue(exception.getMessage().contains("does not belong to course"));
    }

    @Test
    @DisplayName("Should throw LearningOutcomeAlreadyInGroupException when LO is already in a group")
    void testCreateGroupLearningOutcome_LearningOutcomeAlreadyInGroup() {
        // Arrange
        UUID existingGroupId = UUID.randomUUID();
        LearningOutcome loAlreadyInGroup = LearningOutcome.builder()
                .id(loId1)
                .code("CLO-1")
                .courseId(courseId)
                .groupLearningOutcomeId(existingGroupId)
                .build();

        CreateGroupLearningOutcomeCommand command = CreateGroupLearningOutcomeCommand.builder()
                .courseId(courseId)
                .name("Group")
                .learningOutcomeIds(List.of(loId1))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadLearningOutcomePort.findById(loId1)).thenReturn(Optional.of(loAlreadyInGroup));

        // Act & Assert
        LearningOutcomeAlreadyInGroupException exception = assertThrows(LearningOutcomeAlreadyInGroupException.class,
                () -> createGroupLearningOutcomeService.createGroupLearningOutcome(command));

        assertEquals(1, exception.getConflictingIds().size());
        assertEquals(loId1, exception.getConflictingIds().get(0));

        verify(saveGroupLearningOutcomePort, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully create a group with empty learning outcomes list")
    void testCreateGroupLearningOutcome_EmptyLearningOutcomesList() {
        // Arrange
        UUID savedGroupId = UUID.randomUUID();
        CreateGroupLearningOutcomeCommand command = CreateGroupLearningOutcomeCommand.builder()
                .courseId(courseId)
                .name("Empty Group")
                .description("Group with no LOs")
                .learningOutcomeIds(List.of())
                .build();

        GroupLearningOutcome savedGroup = GroupLearningOutcome.builder()
                .id(savedGroupId)
                .name("Empty Group")
                .description("Group with no LOs")
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(saveGroupLearningOutcomePort.save(any(GroupLearningOutcome.class))).thenReturn(savedGroup);

        // Act
        CreateGroupLearningOutcomeResponse response = createGroupLearningOutcomeService.createGroupLearningOutcome(command);

        // Assert
        assertNotNull(response);
        assertEquals(savedGroupId, response.getId());
        assertEquals("Empty Group", response.getName());
        assertEquals(0, response.getLearningOutcomes().size());

        verify(saveLearningOutcomePort, never()).assignToGroup(any(), any(), any());
    }
}
