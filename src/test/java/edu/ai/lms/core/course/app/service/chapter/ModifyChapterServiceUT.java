package edu.ai.lms.core.course.app.service.chapter;

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

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.ModifyChapterResponse;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.ChapterNotFoundException;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.ModifyChapterCommand;
import edu.ai.lms.core.course.app.port.out.LoadChapterPort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveChapterPort;
import edu.ai.lms.core.course.app.service.ccct.CcctCourseChapterSynchronizer;

@ExtendWith(MockitoExtension.class)
class ModifyChapterServiceUT {

    @Mock
    private SaveChapterPort saveChapterPort;

    @Mock
    private LoadChapterPort loadChapterPort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private CcctCourseChapterSynchronizer ccctSynchronizer;

    @InjectMocks
    private ModifyChapterService modifyChapterService;

    private CurrentActor mockUser;
    private UUID teacherId;
    private UUID courseId;
    private UUID chapterId;
    private Course mockCourse;
    private Chapter existingChapter;
    private ModifyChapterCommand validCommand;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        chapterId = UUID.randomUUID();

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

        existingChapter = Chapter.builder()
                .chapterId(chapterId)
                .title("Chapter 1: Introduction")
                .description("Introduction to Java programming")
                .courseId(courseId)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        Chapter updatedChapterData = Chapter.builder()
                .title("Chapter 1: Updated Introduction")
                .description("Updated introduction to Java programming")
                .build();

        validCommand = ModifyChapterCommand.builder()
                .courseId(courseId)
                .chapterId(chapterId)
                .chapter(updatedChapterData)
                .build();
    }

    @Test
    @DisplayName("Should successfully modify a chapter with valid details")
    void testModifyChapter_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadChapterPort.findByKey(courseId, chapterId)).thenReturn(Optional.of(existingChapter));

        Chapter savedChapter = Chapter.builder()
                .chapterId(chapterId)
                .title("Chapter 1: Updated Introduction")
                .description("Updated introduction to Java programming")
                .courseId(courseId)
                .createdAt(existingChapter.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        when(saveChapterPort.save(any(Chapter.class))).thenReturn(savedChapter);

        // Act
        ModifyChapterResponse response = modifyChapterService.modifyChapter(validCommand);

        // Assert
        assertNotNull(response);
        assertEquals(chapterId, response.getChapterId());
        assertEquals("Chapter 1: Updated Introduction", response.getTitle());
        assertEquals("Updated introduction to Java programming", response.getDescription());
        assertEquals(courseId, response.getCourseId());
        assertNotNull(response.getUpdatedAt());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadChapterPort, times(1)).findByKey(courseId, chapterId);
        verify(saveChapterPort, times(1)).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher is not authenticated")
    void testModifyChapter_WithNonAuthenticatedTeacher_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        TeacherNotFoundException exception = assertThrows(TeacherNotFoundException.class,
                () -> modifyChapterService.modifyChapter(validCommand));

        assertEquals("Teacher not authenticated", exception.getMessage());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, never()).findById(any());
        verify(loadChapterPort, never()).findByKey(any(), any());
        verify(saveChapterPort, never()).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testModifyChapter_WithNonExistentCourse_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        // Act & Assert
        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class,
                () -> modifyChapterService.modifyChapter(validCommand));

        assertTrue(exception.getMessage().contains("Course not found with id:"));

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadChapterPort, never()).findByKey(any(), any());
        verify(saveChapterPort, never()).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher is not authorized for the course")
    void testModifyChapter_WithUnauthorizedTeacher_ShouldThrowException() {
        // Arrange
        CurrentActor anotherUser = CurrentActor.builder()
                .id(UUID.randomUUID())
                .role(Roles.TEACHER)
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(anotherUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> modifyChapterService.modifyChapter(validCommand));

        assertEquals("Teacher is not authorized to modify chapters in this course", exception.getMessage());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadChapterPort, never()).findByKey(any(), any());
        verify(saveChapterPort, never()).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Should throw ChapterNotFoundException when chapter does not exist")
    void testModifyChapter_WithNonExistentChapter_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadChapterPort.findByKey(courseId, chapterId)).thenReturn(Optional.empty());

        // Act & Assert
        ChapterNotFoundException exception = assertThrows(ChapterNotFoundException.class,
                () -> modifyChapterService.modifyChapter(validCommand));

        assertTrue(exception.getMessage().contains("Chapter not found with number:"));

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadChapterPort, times(1)).findByKey(courseId, chapterId);
        verify(saveChapterPort, never()).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Should preserve chapterId, courseId, and createdAt when modifying")
    void testModifyChapter_ShouldPreserveExistingFields() {
        // Arrange
        ArgumentCaptor<Chapter> chapterCaptor = ArgumentCaptor.forClass(Chapter.class);
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadChapterPort.findByKey(courseId, chapterId)).thenReturn(Optional.of(existingChapter));

        Chapter savedChapter = Chapter.builder()
                .chapterId(chapterId)
                .title("Chapter 1: Updated Introduction")
                .description("Updated introduction to Java programming")
                .courseId(courseId)
                .createdAt(existingChapter.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        when(saveChapterPort.save(any(Chapter.class))).thenReturn(savedChapter);

        // Act
        modifyChapterService.modifyChapter(validCommand);

        // Assert
        verify(saveChapterPort, times(1)).save(chapterCaptor.capture());
        Chapter capturedChapter = chapterCaptor.getValue();

        assertEquals(chapterId, capturedChapter.getChapterId());
        assertEquals(courseId, capturedChapter.getCourseId());
        assertEquals(existingChapter.getCreatedAt(), capturedChapter.getCreatedAt());
        assertEquals("Chapter 1: Updated Introduction", capturedChapter.getTitle());
        assertEquals("Updated introduction to Java programming", capturedChapter.getDescription());
    }

    @Test
    @DisplayName("Should handle null description when modifying chapter")
    void testModifyChapter_WithNullDescription_ShouldSucceed() {
        // Arrange
        Chapter chapterNullDescription = Chapter.builder()
                .title("Updated Title Only")
                .description(null)
                .build();

        ModifyChapterCommand commandNullDesc = ModifyChapterCommand.builder()
                .courseId(courseId)
                .chapterId(chapterId)
                .chapter(chapterNullDescription)
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadChapterPort.findByKey(courseId, chapterId)).thenReturn(Optional.of(existingChapter));

        Chapter savedChapter = Chapter.builder()
                .chapterId(chapterId)
                .title("Updated Title Only")
                .description(null)
                .courseId(courseId)
                .createdAt(existingChapter.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        when(saveChapterPort.save(any(Chapter.class))).thenReturn(savedChapter);

        // Act
        ModifyChapterResponse response = modifyChapterService.modifyChapter(commandNullDesc);

        // Assert
        assertNotNull(response);
        assertEquals("Updated Title Only", response.getTitle());
        assertEquals(null, response.getDescription());

        verify(saveChapterPort, times(1)).save(any(Chapter.class));
    }
}
