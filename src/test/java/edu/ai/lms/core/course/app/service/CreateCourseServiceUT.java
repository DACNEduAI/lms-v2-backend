package edu.ai.lms.core.course.app.service;

import java.time.LocalDate;
import java.util.ArrayList;
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

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.course.adapter.in.response.CreateCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CourseCodeAlreadyExistsException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.CreateCourseCommand;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;

@ExtendWith(MockitoExtension.class)
class CreateCourseServiceUT {

    @Mock
    private SaveCoursePort saveCoursePort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CreateCourseService createCourseService;

    private CreateCourseCommand validCommand;
    private UserAuth mockUser;
    private UUID teacherId;
    private UUID courseId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        mockUser = UserAuth.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        List<LearningOutcome> learningOutcomes = new ArrayList<>();
        learningOutcomes.add(LearningOutcome.builder()
                .code("CLO-1")
                .description("Understand basic concepts")
                .build());

        Course course = Course.builder()
                .courseName("Introduction to Java")
                .description("Learn Java programming basics")
                .courseCode("CS101")
                .active(true)
                .coverImage("https://example.com/image.jpg")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .learningOutcomes(learningOutcomes)
                .build();

        validCommand = CreateCourseCommand.builder()
                .course(course)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a new course with valid details")
    void testCreateCourse_WithValidDetails_ShouldSucceed() {
        // Arrange
        Course course = validCommand.course();
        when(loadCoursePort.findByCourseCode(course.getCourseCode())).thenReturn(Optional.empty());
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(course.getCourseName())
                .courseCode(course.getCourseCode())
                .description(course.getDescription())
                .teacherIds(List.of(teacherId))
                .active(true)
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        CreateCourseResponse response = createCourseService.createCourse(validCommand);

        // Assert
        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals("Introduction to Java", response.getCourseName());
        assertEquals("CS101", response.getCourseCode());
        
        verify(loadCoursePort, times(1)).findByCourseCode("CS101");
        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(saveCoursePort, times(1)).save(any(Course.class));
    }

    @Test
    @DisplayName("Should throw CourseCodeAlreadyExistsException when course code exists")
    void testCreateCourse_WithExistingCourseCode_ShouldThrowException() {
        // Arrange
        Course existingCourse = Course.builder()
                .id(UUID.randomUUID())
                .courseCode("CS101")
                .build();
        when(loadCoursePort.findByCourseCode(validCommand.course().getCourseCode())).thenReturn(Optional.of(existingCourse));

        // Act & Assert
        CourseCodeAlreadyExistsException exception = assertThrows(CourseCodeAlreadyExistsException.class,
                () -> createCourseService.createCourse(validCommand));

        assertEquals("Course code already exists: CS101", exception.getMessage());
        
        verify(loadCoursePort, times(1)).findByCourseCode("CS101");
        verify(loadSignedInUserPort, never()).findAuthenticatedUser();
        verify(saveCoursePort, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher does not exist")
    void testCreateCourse_WithNonExistentTeacher_ShouldThrowException() {
        // Arrange
        when(loadCoursePort.findByCourseCode(validCommand.course().getCourseCode())).thenReturn(Optional.empty());
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        TeacherNotFoundException exception = assertThrows(TeacherNotFoundException.class,
                () -> createCourseService.createCourse(validCommand));

        assertEquals("Teacher not authenticated", exception.getMessage());
        
        verify(loadCoursePort, times(1)).findByCourseCode("CS101");
        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(saveCoursePort, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Should save course with correct details including learning outcomes")
    void testCreateCourse_ShouldSaveCourseWithCorrectDetails() {
        // Arrange
        Course course = validCommand.course();
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        when(loadCoursePort.findByCourseCode(course.getCourseCode())).thenReturn(Optional.empty());
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(course.getCourseName())
                .courseCode(course.getCourseCode())
                .teacherIds(List.of(teacherId))
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        createCourseService.createCourse(validCommand);

        // Assert
        verify(saveCoursePort, times(1)).save(courseCaptor.capture());
        Course capturedCourse = courseCaptor.getValue();
        
        assertEquals("Introduction to Java", capturedCourse.getCourseName());
        assertEquals("CS101", capturedCourse.getCourseCode());
        assertEquals("Learn Java programming basics", capturedCourse.getDescription());
        assertTrue(capturedCourse.getTeacherIds().contains(teacherId));
        assertTrue(capturedCourse.getActive());
        assertNotNull(capturedCourse.getLearningOutcomes());
        assertEquals(1, capturedCourse.getLearningOutcomes().size());
        assertEquals("CLO-1", capturedCourse.getLearningOutcomes().get(0).getCode());
    }

    @Test
    @DisplayName("Should handle course creation with empty learning outcomes")
    void testCreateCourse_WithEmptyLearningOutcomes_ShouldSucceed() {
        // Arrange
        Course emptyLoCourse = Course.builder()
                .courseName("Introduction to Java")
                .description("Learn Java programming basics")
                .courseCode("CS101")
                .active(true)
                .coverImage("https://example.com/image.jpg")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .learningOutcomes(new ArrayList<>())
                .build();
        CreateCourseCommand emptyLoCommand = CreateCourseCommand.builder()
                .course(emptyLoCourse)
                .build();
        when(loadCoursePort.findByCourseCode(emptyLoCourse.getCourseCode())).thenReturn(Optional.empty());
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(emptyLoCourse.getCourseName())
                .courseCode(emptyLoCourse.getCourseCode())
                .teacherIds(List.of(teacherId))
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        CreateCourseResponse response = createCourseService.createCourse(emptyLoCommand);

        // Assert
        assertNotNull(response);
        verify(saveCoursePort, times(1)).save(any(Course.class));
    }

    @Test
    @DisplayName("Should return correct response with all fields populated")
    void testCreateCourse_ShouldReturnCorrectResponse() {
        // Arrange
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusMonths(3);
        Course course = validCommand.course();
        course.setStartDate(startDate);
        course.setEndDate(endDate);
        
        when(loadCoursePort.findByCourseCode(course.getCourseCode())).thenReturn(Optional.empty());
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(course.getCourseName())
                .courseCode(course.getCourseCode())
                .description(course.getDescription())
                .teacherIds(List.of(teacherId))
                .active(true)
                .coverImage("https://example.com/image.jpg")
                .startDate(startDate)
                .endDate(endDate)
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        CreateCourseResponse response = createCourseService.createCourse(validCommand);

        // Assert
        assertEquals(courseId, response.getCourseId());
        assertEquals("Introduction to Java", response.getCourseName());
        assertEquals("CS101", response.getCourseCode());
        assertEquals("Learn Java programming basics", response.getDescription());
        assertEquals(teacherId, response.getTeacherId());
        assertTrue(response.getActive());
        assertEquals("https://example.com/image.jpg", response.getCoverImage());
        assertEquals(startDate, response.getStartDate());
        assertEquals(endDate, response.getEndDate());
    }
}
