package edu.ai.lms.core.course.app.service;

import edu.ai.lms.core.course.adapter.in.request.CreateCourseRequest;
import edu.ai.lms.core.course.adapter.in.request.CreateLearningOutcomeRequest;
import edu.ai.lms.core.course.adapter.in.response.CreateCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInTeacherPort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;
import edu.ai.lms.core.course.exception.CourseCodeAlreadyExistsException;
import edu.ai.lms.core.course.exception.TeacherNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCourseServiceUT {

    @Mock
    private SaveCoursePort saveCoursePort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInTeacherPort loadSignedInTeacherPort;

    @InjectMocks
    private CreateCourseService createCourseService;

    private CreateCourseRequest validRequest;
    private Teacher mockTeacher;
    private UUID teacherId;
    private UUID courseId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        mockTeacher = Teacher.builder()
                .id(teacherId)
                .employeeId("EMP001")
                .department("Computer Science")
                .specialization("Software Engineering")
                .build();

        List<CreateLearningOutcomeRequest> learningOutcomes = new ArrayList<>();
        learningOutcomes.add(CreateLearningOutcomeRequest.builder()
                .code("CLO-1")
                .description("Understand basic concepts")
                .build());

        validRequest = CreateCourseRequest.builder()
                .courseName("Introduction to Java")
                .description("Learn Java programming basics")
                .courseCode("CS101")
                .active(true)
                .coverImage("https://example.com/image.jpg")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .learningOutcomes(learningOutcomes)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a new course with valid details")
    void testCreateCourse_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadCoursePort.findByCourseCode(validRequest.getCourseCode())).thenReturn(Optional.empty());
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(mockTeacher));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(validRequest.getCourseName())
                .courseCode(validRequest.getCourseCode())
                .description(validRequest.getDescription())
                .teacher(mockTeacher)
                .active(true)
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        CreateCourseResponse response = createCourseService.createCourse(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals("Introduction to Java", response.getCourseName());
        assertEquals("CS101", response.getCourseCode());
        
        verify(loadCoursePort, times(1)).findByCourseCode("CS101");
        verify(loadSignedInTeacherPort, times(1)).findAuthenticatedTeacher();
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
        when(loadCoursePort.findByCourseCode(validRequest.getCourseCode())).thenReturn(Optional.of(existingCourse));

        // Act & Assert
        CourseCodeAlreadyExistsException exception = assertThrows(CourseCodeAlreadyExistsException.class,
                () -> createCourseService.createCourse(validRequest));

        assertEquals("Course code already exists: CS101", exception.getMessage());
        
        verify(loadCoursePort, times(1)).findByCourseCode("CS101");
        verify(loadSignedInTeacherPort, never()).findAuthenticatedTeacher();
        verify(saveCoursePort, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher does not exist")
    void testCreateCourse_WithNonExistentTeacher_ShouldThrowException() {
        // Arrange
        when(loadCoursePort.findByCourseCode(validRequest.getCourseCode())).thenReturn(Optional.empty());
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.empty());

        // Act & Assert
        TeacherNotFoundException exception = assertThrows(TeacherNotFoundException.class,
                () -> createCourseService.createCourse(validRequest));

        assertEquals("Teacher not authenticated", exception.getMessage());
        
        verify(loadCoursePort, times(1)).findByCourseCode("CS101");
        verify(loadSignedInTeacherPort, times(1)).findAuthenticatedTeacher();
        verify(saveCoursePort, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Should save course with correct details including learning outcomes")
    void testCreateCourse_ShouldSaveCourseWithCorrectDetails() {
        // Arrange
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        when(loadCoursePort.findByCourseCode(validRequest.getCourseCode())).thenReturn(Optional.empty());
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(mockTeacher));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(validRequest.getCourseName())
                .courseCode(validRequest.getCourseCode())
                .teacher(mockTeacher)
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        createCourseService.createCourse(validRequest);

        // Assert
        verify(saveCoursePort, times(1)).save(courseCaptor.capture());
        Course capturedCourse = courseCaptor.getValue();
        
        assertEquals("Introduction to Java", capturedCourse.getCourseName());
        assertEquals("CS101", capturedCourse.getCourseCode());
        assertEquals("Learn Java programming basics", capturedCourse.getDescription());
        assertEquals(mockTeacher, capturedCourse.getTeacher());
        assertTrue(capturedCourse.getActive());
        assertNotNull(capturedCourse.getLearningOutcomes());
        assertEquals(1, capturedCourse.getLearningOutcomes().size());
        assertEquals("CLO-1", capturedCourse.getLearningOutcomes().get(0).getCode());
    }

    @Test
    @DisplayName("Should handle course creation with empty learning outcomes")
    void testCreateCourse_WithEmptyLearningOutcomes_ShouldSucceed() {
        // Arrange
        validRequest.setLearningOutcomes(new ArrayList<>());
        when(loadCoursePort.findByCourseCode(validRequest.getCourseCode())).thenReturn(Optional.empty());
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(mockTeacher));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(validRequest.getCourseName())
                .courseCode(validRequest.getCourseCode())
                .teacher(mockTeacher)
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        CreateCourseResponse response = createCourseService.createCourse(validRequest);

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
        validRequest.setStartDate(startDate);
        validRequest.setEndDate(endDate);
        
        when(loadCoursePort.findByCourseCode(validRequest.getCourseCode())).thenReturn(Optional.empty());
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(mockTeacher));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(validRequest.getCourseName())
                .courseCode(validRequest.getCourseCode())
                .description(validRequest.getDescription())
                .teacher(mockTeacher)
                .active(true)
                .coverImage("https://example.com/image.jpg")
                .startDate(startDate)
                .endDate(endDate)
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        CreateCourseResponse response = createCourseService.createCourse(validRequest);

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
