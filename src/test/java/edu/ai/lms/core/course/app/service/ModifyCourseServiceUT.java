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
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.course.adapter.in.request.CreateLearningOutcomeRequest;
import edu.ai.lms.core.course.adapter.in.request.ModifyCourseRequest;
import edu.ai.lms.core.course.adapter.in.response.ModifyCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInTeacherPort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;

@ExtendWith(MockitoExtension.class)
class ModifyCourseServiceUT {

    @Mock
    private SaveCoursePort saveCoursePort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInTeacherPort loadSignedInTeacherPort;

    @InjectMocks
    private ModifyCourseService modifyCourseService;

    private ModifyCourseRequest validRequest;
    private Teacher mockTeacher;
    private Course existingCourse;
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

        existingCourse = Course.builder()
                .id(courseId)
                .courseName("Old Course Name")
                .courseCode("CS101")
                .description("Old description")
                .teacher(mockTeacher)
                .active(true)
                .build();

        List<CreateLearningOutcomeRequest> learningOutcomes = new ArrayList<>();
        learningOutcomes.add(CreateLearningOutcomeRequest.builder()
                .code("CLO-1")
                .description("Updated learning outcome")
                .build());

        validRequest = ModifyCourseRequest.builder()
                .courseId(courseId)
                .courseName("Updated Course Name")
                .description("Updated description")
                .courseCode("CS101")
                .active(true)
                .coverImage("https://example.com/new-image.jpg")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .learningOutcomes(learningOutcomes)
                .build();
    }

    @Test
    @DisplayName("Should successfully modify course with valid details")
    void testModifyCourse_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(mockTeacher));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(validRequest.getCourseName())
                .courseCode(validRequest.getCourseCode())
                .description(validRequest.getDescription())
                .teacher(mockTeacher)
                .active(true)
                .coverImage(validRequest.getCoverImage())
                .startDate(validRequest.getStartDate())
                .endDate(validRequest.getEndDate())
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        ModifyCourseResponse response = modifyCourseService.modifyCourse(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals("Updated Course Name", response.getCourseName());
        assertEquals("CS101", response.getCourseCode());
        assertEquals("Updated description", response.getDescription());
        
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadSignedInTeacherPort, times(1)).findAuthenticatedTeacher();
        verify(saveCoursePort, times(1)).save(any(Course.class));
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testModifyCourse_WithNonExistentCourse_ShouldThrowException() {
        // Arrange
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        // Act & Assert
        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class,
                () -> modifyCourseService.modifyCourse(validRequest));

        assertEquals("Course not found with id: " + courseId, exception.getMessage());
        
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadSignedInTeacherPort, never()).findAuthenticatedTeacher();
        verify(saveCoursePort, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher does not exist")
    void testModifyCourse_WithNonExistentTeacher_ShouldThrowException() {
        // Arrange
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.empty());

        // Act & Assert
        TeacherNotFoundException exception = assertThrows(TeacherNotFoundException.class,
                () -> modifyCourseService.modifyCourse(validRequest));

        assertEquals("Authenticated teacher not found", exception.getMessage());
        
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadSignedInTeacherPort, times(1)).findAuthenticatedTeacher();
        verify(saveCoursePort, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Should throw exception when teacher is not authorized to modify course")
    void testModifyCourse_WithUnauthorizedTeacher_ShouldThrowException() {
        // Arrange
        UUID differentTeacherId = UUID.randomUUID();
        Teacher differentTeacher = Teacher.builder()
                .id(differentTeacherId)
                .employeeId("EMP002")
                .build();
        
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(differentTeacher));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> modifyCourseService.modifyCourse(validRequest));

        assertEquals("Teacher is not authorized to modify this course", exception.getMessage());
        
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadSignedInTeacherPort, times(1)).findAuthenticatedTeacher();
        verify(saveCoursePort, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Should save course with updated details and learning outcomes")
    void testModifyCourse_ShouldSaveCourseWithUpdatedDetails() {
        // Arrange
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(mockTeacher));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(validRequest.getCourseName())
                .courseCode(validRequest.getCourseCode())
                .teacher(mockTeacher)
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        modifyCourseService.modifyCourse(validRequest);

        // Assert
        verify(saveCoursePort, times(1)).save(courseCaptor.capture());
        Course capturedCourse = courseCaptor.getValue();
        
        assertEquals(courseId, capturedCourse.getId());
        assertEquals("Updated Course Name", capturedCourse.getCourseName());
        assertEquals("Updated description", capturedCourse.getDescription());
        assertEquals(mockTeacher, capturedCourse.getTeacher());
        assertNotNull(capturedCourse.getLearningOutcomes());
        assertEquals(1, capturedCourse.getLearningOutcomes().size());
        assertEquals("CLO-1", capturedCourse.getLearningOutcomes().get(0).getCode());
    }

    @Test
    @DisplayName("Should return response with updatedAt timestamp")
    void testModifyCourse_ShouldReturnResponseWithUpdatedAt() {
        // Arrange
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(mockTeacher));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(validRequest.getCourseName())
                .courseCode(validRequest.getCourseCode())
                .teacher(mockTeacher)
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        ModifyCourseResponse response = modifyCourseService.modifyCourse(validRequest);

        // Assert
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle modify course with empty learning outcomes")
    void testModifyCourse_WithEmptyLearningOutcomes_ShouldSucceed() {
        // Arrange
        validRequest.setLearningOutcomes(new ArrayList<>());
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(mockTeacher));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(validRequest.getCourseName())
                .courseCode(validRequest.getCourseCode())
                .teacher(mockTeacher)
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        ModifyCourseResponse response = modifyCourseService.modifyCourse(validRequest);

        // Assert
        assertNotNull(response);
        verify(saveCoursePort, times(1)).save(any(Course.class));
    }
}
