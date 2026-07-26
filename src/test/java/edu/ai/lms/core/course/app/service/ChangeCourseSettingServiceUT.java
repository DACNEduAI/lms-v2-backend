package edu.ai.lms.core.course.app.service;

import java.time.LocalDate;
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
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.course.adapter.in.response.ChangeCourseSettingResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.ChangeCourseSettingCommand;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;

@ExtendWith(MockitoExtension.class)
class ChangeCourseSettingServiceUT {

    @Mock
    private SaveCoursePort saveCoursePort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @InjectMocks
    private ChangeCourseSettingService changeCourseSettingService;

    private ChangeCourseSettingCommand validCommand;
    private Course existingCourse;
    private UUID teacherId;
    private UUID courseId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        existingCourse = Course.builder()
                .id(courseId)
                .courseName("Original Course Name")
                .courseCode("CS101")
                .description("Original description")
                .teacherIds(List.of(teacherId))
                .active(true)
                .coverImage("https://example.com/old-image.jpg")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .build();

        validCommand = ChangeCourseSettingCommand.builder()
                .courseId(courseId)
                .courseName("Updated Course Name")
                .description("Updated description")
                .active(false)
                .coverImage("https://example.com/new-image.jpg")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusMonths(6))
                .build();
    }

    @Test
    @DisplayName("Should successfully change course settings with valid details")
    void testChangeCourseSetting_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(validCommand.courseName())
                .courseCode(existingCourse.getCourseCode())
                .description(validCommand.description())
                .teacherIds(List.of(teacherId))
                .active(validCommand.active())
                .coverImage(validCommand.coverImage())
                .startDate(validCommand.startDate())
                .endDate(validCommand.endDate())
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        ChangeCourseSettingResponse response = changeCourseSettingService.changeCourseSetting(validCommand);

        // Assert
        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals("Updated Course Name", response.getCourseName());
        assertEquals("Updated description", response.getDescription());
        assertFalse(response.getActive());
        
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(saveCoursePort, times(1)).save(any(Course.class));
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testChangeCourseSetting_WithNonExistentCourse_ShouldThrowException() {
        // Arrange
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        // Act & Assert
        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class,
                () -> changeCourseSettingService.changeCourseSetting(validCommand));

        assertEquals("Course not found with id: " + courseId, exception.getMessage());
        
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(saveCoursePort, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Should only update provided non-null fields")
    void testChangeCourseSetting_ShouldOnlyUpdateNonNullFields() {
        // Arrange
        ChangeCourseSettingCommand partialCommand = ChangeCourseSettingCommand.builder()
                .courseId(courseId)
                .courseName("Only Name Updated")
                .build();
        
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(saveCoursePort.save(any(Course.class))).thenReturn(existingCourse);

        // Act
        changeCourseSettingService.changeCourseSetting(partialCommand);

        // Assert
        verify(saveCoursePort, times(1)).save(courseCaptor.capture());
        Course capturedCourse = courseCaptor.getValue();
        
        assertEquals("Only Name Updated", capturedCourse.getCourseName());
        assertEquals("Original description", capturedCourse.getDescription());
        assertTrue(capturedCourse.getActive());
        assertEquals("https://example.com/old-image.jpg", capturedCourse.getCoverImage());
    }

    @Test
    @DisplayName("Should update course name when provided")
    void testChangeCourseSetting_ShouldUpdateCourseName() {
        // Arrange
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(saveCoursePort.save(any(Course.class))).thenReturn(existingCourse);

        // Act
        changeCourseSettingService.changeCourseSetting(validCommand);

        // Assert
        verify(saveCoursePort).save(courseCaptor.capture());
        assertEquals("Updated Course Name", courseCaptor.getValue().getCourseName());
    }

    @Test
    @DisplayName("Should update description when provided")
    void testChangeCourseSetting_ShouldUpdateDescription() {
        // Arrange
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(saveCoursePort.save(any(Course.class))).thenReturn(existingCourse);

        // Act
        changeCourseSettingService.changeCourseSetting(validCommand);

        // Assert
        verify(saveCoursePort).save(courseCaptor.capture());
        assertEquals("Updated description", courseCaptor.getValue().getDescription());
    }

    @Test
    @DisplayName("Should update active status when provided")
    void testChangeCourseSetting_ShouldUpdateActiveStatus() {
        // Arrange
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(saveCoursePort.save(any(Course.class))).thenReturn(existingCourse);

        // Act
        changeCourseSettingService.changeCourseSetting(validCommand);

        // Assert
        verify(saveCoursePort).save(courseCaptor.capture());
        assertFalse(courseCaptor.getValue().getActive());
    }

    @Test
    @DisplayName("Should update cover image when provided")
    void testChangeCourseSetting_ShouldUpdateCoverImage() {
        // Arrange
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(saveCoursePort.save(any(Course.class))).thenReturn(existingCourse);

        // Act
        changeCourseSettingService.changeCourseSetting(validCommand);

        // Assert
        verify(saveCoursePort).save(courseCaptor.capture());
        assertEquals("https://example.com/new-image.jpg", courseCaptor.getValue().getCoverImage());
    }

    @Test
    @DisplayName("Should update dates when provided")
    void testChangeCourseSetting_ShouldUpdateDates() {
        // Arrange
        LocalDate newStartDate = LocalDate.now().plusDays(1);
        LocalDate newEndDate = LocalDate.now().plusMonths(6);
        ChangeCourseSettingCommand dateCommand = ChangeCourseSettingCommand.builder()
                .courseId(courseId)
                .courseName(validCommand.courseName())
                .description(validCommand.description())
                .active(validCommand.active())
                .coverImage(validCommand.coverImage())
                .startDate(newStartDate)
                .endDate(newEndDate)
                .build();
        
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(saveCoursePort.save(any(Course.class))).thenReturn(existingCourse);

        // Act
        changeCourseSettingService.changeCourseSetting(dateCommand);

        // Assert
        verify(saveCoursePort).save(courseCaptor.capture());
        assertEquals(newStartDate, courseCaptor.getValue().getStartDate());
        assertEquals(newEndDate, courseCaptor.getValue().getEndDate());
    }

    @Test
    @DisplayName("Should return correct response with all fields")
    void testChangeCourseSetting_ShouldReturnCorrectResponse() {
        // Arrange
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName("Updated Course Name")
                .courseCode("CS101")
                .description("Updated description")
                .teacherIds(List.of(teacherId))
                .active(false)
                .coverImage("https://example.com/new-image.jpg")
                .startDate(validCommand.startDate())
                .endDate(validCommand.endDate())
                .build();
        
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        ChangeCourseSettingResponse response = changeCourseSettingService.changeCourseSetting(validCommand);

        // Assert
        assertEquals(courseId, response.getCourseId());
        assertEquals("Updated Course Name", response.getCourseName());
        assertEquals("CS101", response.getCourseCode());
        assertEquals("Updated description", response.getDescription());
        assertFalse(response.getActive());
        assertEquals("https://example.com/new-image.jpg", response.getCoverImage());
    }

    @Test
    @DisplayName("Should preserve course code which cannot be changed")
    void testChangeCourseSetting_ShouldPreserveCourseCode() {
        // Arrange
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(existingCourse));
        
        Course savedCourse = Course.builder()
                .id(courseId)
                .courseName(validCommand.courseName())
                .courseCode("CS101")
                .teacherIds(List.of(teacherId))
                .build();
        when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        ChangeCourseSettingResponse response = changeCourseSettingService.changeCourseSetting(validCommand);

        // Assert
        assertEquals("CS101", response.getCourseCode());
    }
}
