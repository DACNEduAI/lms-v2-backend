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

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.course.app.domain.CurrentActor;
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

    private CurrentActor mockTeacher;
    private CurrentActor mockStudent;
    private UUID teacherId;
    private UUID studentId;
    private UUID courseId;
    private UUID groupId;
    private Course mockCourse;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        groupId = UUID.randomUUID();

        mockTeacher = CurrentActor.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        mockStudent = CurrentActor.builder()
                .id(studentId)
                .role(Roles.STUDENT)
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
                .studentIds(List.of(studentId))
                .learningOutcomes(learningOutcomes)
                .build();
    }

    @Test
    @DisplayName("Should successfully get a course with grouped and ungrouped learning outcomes")
    void testGetCourse_WithValidCourseIdAndAuthenticatedTeacher_ShouldSucceed() {
        GroupLearningOutcome group = GroupLearningOutcome.builder()
                .id(groupId)
                .name("Fundamentals")
                .description("Basic concepts group")
                .courseId(courseId)
                .learningOutcomes(List.of(mockCourse.getLearningOutcomes().get(0)))
                .build();

        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadGroupLearningOutcomePort.findByCourseId(courseId)).thenReturn(List.of(group));

        GetCourseResponse response = getCourseService.getCourse(courseId);

        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals("Introduction to Java", response.getCourseName());
        assertEquals("CS101", response.getCourseCode());
        assertEquals(teacherId, response.getTeacherId());
        assertTrue(response.getActive());
        assertEquals(1, response.getGroupLearningOutcomes().size());
        assertEquals(1, response.getUngroupedLearningOutcomes().size());

        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadGroupLearningOutcomePort, times(1)).findByCourseId(courseId);
    }

    @Test
    @DisplayName("Should allow enrolled student to access the course")
    void testGetCourse_WithEnrolledStudent_ShouldSucceed() {
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
        when(loadGroupLearningOutcomePort.findByCourseId(courseId)).thenReturn(Collections.emptyList());

        GetCourseResponse response = getCourseService.getCourse(courseId);

        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals(teacherId, response.getTeacherId());
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testGetCourse_WithNonExistentCourse_ShouldThrowException() {
        UUID nonExistentCourseId = UUID.randomUUID();
        when(loadCoursePort.findById(nonExistentCourseId)).thenReturn(Optional.empty());

                CourseNotFoundException exception = assertThrows(CourseNotFoundException.class,
                () -> getCourseService.getCourse(nonExistentCourseId));
                assertNotNull(exception);

        verify(loadCoursePort, times(1)).findById(nonExistentCourseId);
        verify(loadSignedInUserPort, never()).findAuthenticatedUser();
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when user is not authenticated")
    void testGetCourse_WithNonAuthenticatedUser_ShouldThrowException() {
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        TeacherNotFoundException exception = assertThrows(TeacherNotFoundException.class,
                () -> getCourseService.getCourse(courseId));

        assertEquals("Authenticated user not found", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not authorized")
    void testGetCourse_WithUnauthorizedUser_ShouldThrowException() {
        CurrentActor anotherUser = CurrentActor.builder()
                .id(UUID.randomUUID())
                .role(Roles.TEACHER)
                .build();

        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(anotherUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> getCourseService.getCourse(courseId));

        assertEquals("User is not authorized to access this course", exception.getMessage());
    }

    @Test
    @DisplayName("Should return empty grouped and ungrouped lists when no learning outcomes")
    void testGetCourse_WithEmptyLearningOutcomes_ShouldSucceed() {
        Course courseWithNoLearningOutcomes = Course.builder()
                .id(courseId)
                .courseName("Empty Course")
                .description("A course with no learning outcomes")
                .courseCode("CS102")
                .active(true)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .teacherIds(List.of(teacherId))
                .studentIds(List.of(studentId))
                .learningOutcomes(new ArrayList<>())
                .build();

        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(courseWithNoLearningOutcomes));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadGroupLearningOutcomePort.findByCourseId(courseId)).thenReturn(Collections.emptyList());

        GetCourseResponse response = getCourseService.getCourse(courseId);

        assertNotNull(response);
        assertEquals(0, response.getGroupLearningOutcomes().size());
        assertEquals(0, response.getUngroupedLearningOutcomes().size());
    }

    @Test
    @DisplayName("Should successfully get an inactive course")
    void testGetCourse_WithInactiveCourse_ShouldSucceed() {
        Course inactiveCourse = Course.builder()
                .id(courseId)
                .courseName("Inactive Course")
                .description("An inactive course")
                .courseCode("CS103")
                .active(false)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .teacherIds(List.of(teacherId))
                .studentIds(List.of(studentId))
                .learningOutcomes(new ArrayList<>())
                .build();

        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(inactiveCourse));
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadGroupLearningOutcomePort.findByCourseId(courseId)).thenReturn(Collections.emptyList());

        GetCourseResponse response = getCourseService.getCourse(courseId);

        assertNotNull(response);
        assertFalse(response.getActive());
    }
}
