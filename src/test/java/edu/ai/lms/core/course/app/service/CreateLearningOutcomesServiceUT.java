package edu.ai.lms.core.course.app.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.CreateLearningOutcomesResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.CreateLearningOutcomesCommand;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveLearningOutcomePort;

@ExtendWith(MockitoExtension.class)
class CreateLearningOutcomesServiceUT {

    @Mock
    private SaveLearningOutcomePort saveLearningOutcomePort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private LoadGroupLearningOutcomePort loadGroupLearningOutcomePort;

    @Mock
    private LoadLearningOutcomePort loadLearningOutcomePort;

    @InjectMocks
    private CreateLearningOutcomesService createLearningOutcomesService;

    private CurrentActor mockUser;
    private UUID teacherId;
    private UUID courseId;
    private Course mockCourse;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();

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
    }

    @Test
    @DisplayName("Should successfully create learning outcomes with valid details")
    void testCreateLearningOutcomes_WithValidDetails_ShouldSucceed() {
        // Arrange
        LearningOutcome lo1 = LearningOutcome.builder().code("CLO-1").description("Understand basics").build();
        LearningOutcome lo2 = LearningOutcome.builder().code("CLO-2").description("Apply concepts").build();

        CreateLearningOutcomesCommand command = CreateLearningOutcomesCommand.builder()
                .courseId(courseId)
                .learningOutcomes(List.of(lo1, lo2))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        UUID savedId1 = UUID.randomUUID();
        UUID savedId2 = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        when(saveLearningOutcomePort.save(any(LearningOutcome.class)))
                .thenReturn(LearningOutcome.builder()
                        .id(savedId1).code("CLO-1").description("Understand basics")
                        .courseId(courseId).createdAt(now).build())
                .thenReturn(LearningOutcome.builder()
                        .id(savedId2).code("CLO-2").description("Apply concepts")
                        .courseId(courseId).createdAt(now).build());

        // Act
        CreateLearningOutcomesResponse response = createLearningOutcomesService.createLearningOutcomes(command);

        // Assert
        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals(2, response.getTotalCreated());
        assertEquals(2, response.getLearningOutcomes().size());
        assertEquals("CLO-1", response.getLearningOutcomes().get(0).getCode());
        assertEquals("CLO-2", response.getLearningOutcomes().get(1).getCode());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(saveLearningOutcomePort, times(2)).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should successfully create a single learning outcome")
    void testCreateLearningOutcomes_WithSingleItem_ShouldSucceed() {
        // Arrange
        LearningOutcome lo = LearningOutcome.builder().code("CLO-1").description("Understand basics").build();

        CreateLearningOutcomesCommand command = CreateLearningOutcomesCommand.builder()
                .courseId(courseId)
                .learningOutcomes(List.of(lo))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        UUID savedId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        when(saveLearningOutcomePort.save(any(LearningOutcome.class)))
                .thenReturn(LearningOutcome.builder()
                        .id(savedId).code("CLO-1").description("Understand basics")
                        .courseId(courseId).createdAt(now).build());

        // Act
        CreateLearningOutcomesResponse response = createLearningOutcomesService.createLearningOutcomes(command);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalCreated());
        assertEquals(1, response.getLearningOutcomes().size());

        verify(saveLearningOutcomePort, times(1)).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should number new grouped learning outcomes from one")
    void testCreateLearningOutcomes_WithNewGroupChildren_ShouldAssignSequentialPrefixNumbers() {
        UUID groupId = UUID.randomUUID();
        GroupLearningOutcome group = GroupLearningOutcome.builder()
                .id(groupId)
                .code("L.O.2")
                .courseId(courseId)
                .build();
        LearningOutcome lo1 = LearningOutcome.builder().description("ReactJS").build();
        LearningOutcome lo2 = LearningOutcome.builder().description("TanStack Query").build();
        CreateLearningOutcomesCommand command = CreateLearningOutcomesCommand.builder()
                .courseId(courseId)
                .groupLearningOutcomeId(groupId)
                .learningOutcomes(List.of(lo1, lo2))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadGroupLearningOutcomePort.findById(groupId)).thenReturn(Optional.of(group));
        when(loadLearningOutcomePort.findMaxSortOrderByGroupId(groupId)).thenReturn(0);
        when(saveLearningOutcomePort.save(any(LearningOutcome.class)))
                .thenAnswer(invocation -> {
                    LearningOutcome saved = invocation.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    saved.setCreatedAt(LocalDateTime.now());
                    return saved;
                });

        CreateLearningOutcomesResponse response = createLearningOutcomesService.createLearningOutcomes(command);

        assertEquals(2, response.getTotalCreated());
        assertEquals(1, response.getLearningOutcomes().get(0).getPrefixNumber());
        assertEquals(2, response.getLearningOutcomes().get(1).getPrefixNumber());
        assertEquals("L.O.2.1", response.getLearningOutcomes().get(0).getDisplayCode());
        assertEquals("L.O.2.2", response.getLearningOutcomes().get(1).getDisplayCode());
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher is not authenticated")
    void testCreateLearningOutcomes_WithNonAuthenticatedTeacher_ShouldThrowException() {
        // Arrange
        LearningOutcome lo = LearningOutcome.builder().code("CLO-1").description("Understand basics").build();
        CreateLearningOutcomesCommand command = CreateLearningOutcomesCommand.builder()
                .courseId(courseId)
                .learningOutcomes(List.of(lo))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        TeacherNotFoundException exception = assertThrows(TeacherNotFoundException.class,
                () -> createLearningOutcomesService.createLearningOutcomes(command));

        assertEquals("Teacher not authenticated", exception.getMessage());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, never()).findById(any());
        verify(saveLearningOutcomePort, never()).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testCreateLearningOutcomes_WithNonExistentCourse_ShouldThrowException() {
        // Arrange
        LearningOutcome lo = LearningOutcome.builder().code("CLO-1").description("Understand basics").build();
        CreateLearningOutcomesCommand command = CreateLearningOutcomesCommand.builder()
                .courseId(courseId)
                .learningOutcomes(List.of(lo))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        // Act & Assert
        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class,
                () -> createLearningOutcomesService.createLearningOutcomes(command));

        assertEquals("Course not found with id: " + courseId, exception.getMessage());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(saveLearningOutcomePort, never()).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher does not own the course")
    void testCreateLearningOutcomes_WithUnauthorizedTeacher_ShouldThrowException() {
        // Arrange
        UUID anotherTeacherId = UUID.randomUUID();
        CurrentActor anotherUser = CurrentActor.builder().id(anotherTeacherId).role(Roles.TEACHER).build();

        LearningOutcome lo = LearningOutcome.builder().code("CLO-1").description("Understand basics").build();
        CreateLearningOutcomesCommand command = CreateLearningOutcomesCommand.builder()
                .courseId(courseId)
                .learningOutcomes(List.of(lo))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(anotherUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createLearningOutcomesService.createLearningOutcomes(command));

        assertEquals("Teacher is not authorized to add learning outcomes to this course", exception.getMessage());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(saveLearningOutcomePort, never()).save(any(LearningOutcome.class));
    }

    @Test
    @DisplayName("Should set courseId on each learning outcome before saving")
    void testCreateLearningOutcomes_ShouldSetCourseIdBeforeSaving() {
        // Arrange
        LearningOutcome lo = LearningOutcome.builder().code("CLO-1").description("Understand basics").build();
        CreateLearningOutcomesCommand command = CreateLearningOutcomesCommand.builder()
                .courseId(courseId)
                .learningOutcomes(List.of(lo))
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        UUID savedId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        when(saveLearningOutcomePort.save(any(LearningOutcome.class)))
                .thenAnswer(invocation -> {
                    LearningOutcome saved = invocation.getArgument(0);
                    assertEquals(courseId, saved.getCourseId(), "CourseId should be set before saving");
                    return LearningOutcome.builder()
                            .id(savedId).code(saved.getCode()).description(saved.getDescription())
                            .courseId(courseId).createdAt(now).build();
                });

        // Act
        CreateLearningOutcomesResponse response = createLearningOutcomesService.createLearningOutcomes(command);

        // Assert
        assertNotNull(response);
        verify(saveLearningOutcomePort, times(1)).save(any(LearningOutcome.class));
    }
}
