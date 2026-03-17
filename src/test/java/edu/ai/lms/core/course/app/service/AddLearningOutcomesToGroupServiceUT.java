package edu.ai.lms.core.course.app.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.User;
import edu.ai.lms.core.course.adapter.in.response.AddLearningOutcomesToGroupResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.GroupLearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeAlreadyInGroupException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.AddLearningOutcomesToGroupCommand;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveLearningOutcomePort;

@ExtendWith(MockitoExtension.class)
class AddLearningOutcomesToGroupServiceUT {

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadGroupLearningOutcomePort loadGroupLearningOutcomePort;

    @Mock
    private LoadLearningOutcomePort loadLearningOutcomePort;

    @Mock
    private SaveLearningOutcomePort saveLearningOutcomePort;

    @InjectMocks
    private AddLearningOutcomesToGroupService addLearningOutcomesToGroupService;

    private User mockUser;
    private UUID teacherId;
    private UUID courseId;
    private UUID groupId;
    private Course mockCourse;
    private GroupLearningOutcome mockGroup;
    private UUID loId1;
    private UUID loId2;
    private LearningOutcome lo1;
    private LearningOutcome lo2;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        loId1 = UUID.randomUUID();
        loId2 = UUID.randomUUID();

        mockUser = User.builder()
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

        mockGroup = GroupLearningOutcome.builder()
                .id(groupId)
                .name("Fundamentals")
                .description("Basic concepts group")
                .courseId(courseId)
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
    @DisplayName("Should successfully add learning outcomes to a group")
    void testAddLearningOutcomesToGroup_Success() {
        // Arrange
        AddLearningOutcomesToGroupCommand command = AddLearningOutcomesToGroupCommand.builder()
                .courseId(courseId)
                .groupLearningOutcomeId(groupId)
                .learningOutcomeIds(List.of(loId1, loId2))
                .build();

        GroupLearningOutcome updatedGroup = GroupLearningOutcome.builder()
                .id(groupId)
                .name("Fundamentals")
                .courseId(courseId)
                .learningOutcomes(List.of(lo1, lo2))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadGroupLearningOutcomePort.findById(groupId)).thenReturn(Optional.of(mockGroup))
                .thenReturn(Optional.of(updatedGroup));
        when(loadLearningOutcomePort.findById(loId1)).thenReturn(Optional.of(lo1));
        when(loadLearningOutcomePort.findById(loId2)).thenReturn(Optional.of(lo2));

        // Act
        AddLearningOutcomesToGroupResponse response = addLearningOutcomesToGroupService.addLearningOutcomesToGroup(command);

        // Assert
        assertNotNull(response);
        assertEquals(groupId, response.getGroupId());
        assertEquals("Fundamentals", response.getName());
        assertEquals(courseId, response.getCourseId());
        assertEquals(2, response.getTotalAdded());
        assertEquals(2, response.getLearningOutcomes().size());

        verify(saveLearningOutcomePort, times(1)).assignToGroup(loId1, courseId, groupId);
        verify(saveLearningOutcomePort, times(1)).assignToGroup(loId2, courseId, groupId);
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher is not authenticated")
    void testAddLearningOutcomesToGroup_TeacherNotAuthenticated() {
        // Arrange
        AddLearningOutcomesToGroupCommand command = AddLearningOutcomesToGroupCommand.builder()
                .courseId(courseId)
                .groupLearningOutcomeId(groupId)
                .learningOutcomeIds(List.of(loId1))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TeacherNotFoundException.class,
                () -> addLearningOutcomesToGroupService.addLearningOutcomesToGroup(command));

        verify(loadCoursePort, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testAddLearningOutcomesToGroup_CourseNotFound() {
        // Arrange
        UUID nonExistentCourseId = UUID.randomUUID();
        AddLearningOutcomesToGroupCommand command = AddLearningOutcomesToGroupCommand.builder()
                .courseId(nonExistentCourseId)
                .groupLearningOutcomeId(groupId)
                .learningOutcomeIds(List.of(loId1))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(nonExistentCourseId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CourseNotFoundException.class,
                () -> addLearningOutcomesToGroupService.addLearningOutcomesToGroup(command));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher is not authorized")
    void testAddLearningOutcomesToGroup_TeacherNotAuthorized() {
        // Arrange
        User anotherUser = User.builder()
                .id(UUID.randomUUID())
                .role(Roles.TEACHER)
                .build();

        AddLearningOutcomesToGroupCommand command = AddLearningOutcomesToGroupCommand.builder()
                .courseId(courseId)
                .groupLearningOutcomeId(groupId)
                .learningOutcomeIds(List.of(loId1))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(anotherUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> addLearningOutcomesToGroupService.addLearningOutcomesToGroup(command));
    }

    @Test
    @DisplayName("Should throw GroupLearningOutcomeNotFoundException when group does not exist")
    void testAddLearningOutcomesToGroup_GroupNotFound() {
        // Arrange
        UUID nonExistentGroupId = UUID.randomUUID();
        AddLearningOutcomesToGroupCommand command = AddLearningOutcomesToGroupCommand.builder()
                .courseId(courseId)
                .groupLearningOutcomeId(nonExistentGroupId)
                .learningOutcomeIds(List.of(loId1))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadGroupLearningOutcomePort.findById(nonExistentGroupId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(GroupLearningOutcomeNotFoundException.class,
                () -> addLearningOutcomesToGroupService.addLearningOutcomesToGroup(command));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when group does not belong to course")
    void testAddLearningOutcomesToGroup_GroupNotInCourse() {
        // Arrange
        UUID otherCourseId = UUID.randomUUID();
        GroupLearningOutcome groupFromOtherCourse = GroupLearningOutcome.builder()
                .id(groupId)
                .name("Other Group")
                .courseId(otherCourseId)
                .build();

        AddLearningOutcomesToGroupCommand command = AddLearningOutcomesToGroupCommand.builder()
                .courseId(courseId)
                .groupLearningOutcomeId(groupId)
                .learningOutcomeIds(List.of(loId1))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadGroupLearningOutcomePort.findById(groupId)).thenReturn(Optional.of(groupFromOtherCourse));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> addLearningOutcomesToGroupService.addLearningOutcomesToGroup(command));
    }

    @Test
    @DisplayName("Should throw LearningOutcomeAlreadyInGroupException when LO is in another group")
    void testAddLearningOutcomesToGroup_LearningOutcomeAlreadyInAnotherGroup() {
        // Arrange
        UUID anotherGroupId = UUID.randomUUID();
        LearningOutcome loInAnotherGroup = LearningOutcome.builder()
                .id(loId1)
                .code("CLO-1")
                .courseId(courseId)
                .groupLearningOutcomeId(anotherGroupId)
                .build();

        AddLearningOutcomesToGroupCommand command = AddLearningOutcomesToGroupCommand.builder()
                .courseId(courseId)
                .groupLearningOutcomeId(groupId)
                .learningOutcomeIds(List.of(loId1))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadGroupLearningOutcomePort.findById(groupId)).thenReturn(Optional.of(mockGroup));
        when(loadLearningOutcomePort.findById(loId1)).thenReturn(Optional.of(loInAnotherGroup));

        // Act & Assert
        LearningOutcomeAlreadyInGroupException exception = assertThrows(LearningOutcomeAlreadyInGroupException.class,
                () -> addLearningOutcomesToGroupService.addLearningOutcomesToGroup(command));

        assertEquals(1, exception.getConflictingIds().size());
        assertEquals(loId1, exception.getConflictingIds().get(0));

        verify(saveLearningOutcomePort, never()).assignToGroup(any(), any(), any());
    }

    @Test
    @DisplayName("Should allow adding LO that is already in the same group")
    void testAddLearningOutcomesToGroup_LearningOutcomeAlreadyInSameGroup() {
        // Arrange
        LearningOutcome loAlreadyInSameGroup = LearningOutcome.builder()
                .id(loId1)
                .code("CLO-1")
                .courseId(courseId)
                .groupLearningOutcomeId(groupId)
                .build();

        AddLearningOutcomesToGroupCommand command = AddLearningOutcomesToGroupCommand.builder()
                .courseId(courseId)
                .groupLearningOutcomeId(groupId)
                .learningOutcomeIds(List.of(loId1))
                .build();

        GroupLearningOutcome updatedGroup = GroupLearningOutcome.builder()
                .id(groupId)
                .name("Fundamentals")
                .courseId(courseId)
                .learningOutcomes(List.of(loAlreadyInSameGroup))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadGroupLearningOutcomePort.findById(groupId)).thenReturn(Optional.of(mockGroup))
                .thenReturn(Optional.of(updatedGroup));
        when(loadLearningOutcomePort.findById(loId1)).thenReturn(Optional.of(loAlreadyInSameGroup));

        // Act
        AddLearningOutcomesToGroupResponse response = addLearningOutcomesToGroupService.addLearningOutcomesToGroup(command);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalAdded());

        verify(saveLearningOutcomePort, times(1)).assignToGroup(loId1, courseId, groupId);
    }

    @Test
    @DisplayName("Should throw LearningOutcomeNotFoundException when LO does not exist")
    void testAddLearningOutcomesToGroup_LearningOutcomeNotFound() {
        // Arrange
        UUID nonExistentLoId = UUID.randomUUID();
        AddLearningOutcomesToGroupCommand command = AddLearningOutcomesToGroupCommand.builder()
                .courseId(courseId)
                .groupLearningOutcomeId(groupId)
                .learningOutcomeIds(List.of(nonExistentLoId))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadGroupLearningOutcomePort.findById(groupId)).thenReturn(Optional.of(mockGroup));
        when(loadLearningOutcomePort.findById(nonExistentLoId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(LearningOutcomeNotFoundException.class,
                () -> addLearningOutcomesToGroupService.addLearningOutcomesToGroup(command));
    }
}
