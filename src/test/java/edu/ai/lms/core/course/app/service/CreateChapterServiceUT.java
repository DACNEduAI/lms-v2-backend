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
import edu.ai.lms.core.auth.app.domain.User;
import edu.ai.lms.core.course.adapter.in.response.CreateChapterResponse;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.CreateChapterCommand;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveChapterPort;

@ExtendWith(MockitoExtension.class)
class CreateChapterServiceUT {

    @Mock
    private SaveChapterPort saveChapterPort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private CreateChapterService createChapterService;

    private User mockUser;
    private UUID teacherId;
    private UUID courseId;
    private Long chapterNumber;
    private Course mockCourse;
    private CreateChapterCommand validCommand;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        chapterNumber = 1L;

        mockUser = User.builder()
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

        Chapter chapter = Chapter.builder()
                .title("Chapter 1: Introduction")
                .description("Introduction to Java programming")
                .build();

        validCommand = CreateChapterCommand.builder()
                .courseId(courseId)
                .chapter(chapter)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a chapter with valid details")
    void testCreateChapter_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        Chapter savedChapter = Chapter.builder()
                .chapterNumber(chapterNumber)
                .title("Chapter 1: Introduction")
                .description("Introduction to Java programming")
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveChapterPort.save(any(Chapter.class))).thenReturn(savedChapter);

        // Act
        CreateChapterResponse response = createChapterService.createChapter(validCommand);

        // Assert
        assertNotNull(response);
        assertEquals(chapterNumber, response.getChapterNumber());
        assertEquals("Chapter 1: Introduction", response.getTitle());
        assertEquals("Introduction to Java programming", response.getDescription());
        assertEquals(courseId, response.getCourseId());
        assertNotNull(response.getCreatedAt());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(saveChapterPort, times(1)).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher is not authenticated")
    void testCreateChapter_WithNonAuthenticatedTeacher_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        TeacherNotFoundException exception = assertThrows(TeacherNotFoundException.class,
                () -> createChapterService.createChapter(validCommand));

        assertEquals("Teacher not authenticated", exception.getMessage());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, never()).findById(any());
        verify(saveChapterPort, never()).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testCreateChapter_WithNonExistentCourse_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        // Act & Assert
        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class,
                () -> createChapterService.createChapter(validCommand));

        assertTrue(exception.getMessage().contains("Course not found with id:"));

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(saveChapterPort, never()).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher is not authorized for the course")
    void testCreateChapter_WithUnauthorizedTeacher_ShouldThrowException() {
        // Arrange
        User anotherUser = User.builder()
                .id(UUID.randomUUID())
                .role(Roles.TEACHER)
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(anotherUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createChapterService.createChapter(validCommand));

        assertEquals("Teacher is not authorized to add chapters to this course", exception.getMessage());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(saveChapterPort, never()).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Should set courseId on chapter before saving")
    void testCreateChapter_ShouldSetCourseIdBeforeSaving() {
        // Arrange
        ArgumentCaptor<Chapter> chapterCaptor = ArgumentCaptor.forClass(Chapter.class);
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        Chapter savedChapter = Chapter.builder()
                .chapterNumber(chapterNumber)
                .title("Chapter 1: Introduction")
                .description("Introduction to Java programming")
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveChapterPort.save(any(Chapter.class))).thenReturn(savedChapter);

        // Act
        createChapterService.createChapter(validCommand);

        // Assert
        verify(saveChapterPort, times(1)).save(chapterCaptor.capture());
        Chapter capturedChapter = chapterCaptor.getValue();

        assertEquals(courseId, capturedChapter.getCourseId());
        assertEquals("Chapter 1: Introduction", capturedChapter.getTitle());
        assertEquals("Introduction to Java programming", capturedChapter.getDescription());
    }

    @Test
    @DisplayName("Should return correct response with all fields populated")
    void testCreateChapter_ShouldReturnCorrectResponse() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        LocalDateTime createdAt = LocalDateTime.now();
        Chapter savedChapter = Chapter.builder()
                .chapterNumber(chapterNumber)
                .title("Chapter 1: Introduction")
                .description("Introduction to Java programming")
                .courseId(courseId)
                .createdAt(createdAt)
                .build();
        when(saveChapterPort.save(any(Chapter.class))).thenReturn(savedChapter);

        // Act
        CreateChapterResponse response = createChapterService.createChapter(validCommand);

        // Assert
        assertEquals(chapterNumber, response.getChapterNumber());
        assertEquals("Chapter 1: Introduction", response.getTitle());
        assertEquals("Introduction to Java programming", response.getDescription());
        assertEquals(courseId, response.getCourseId());
        assertEquals(createdAt, response.getCreatedAt());
    }
}

