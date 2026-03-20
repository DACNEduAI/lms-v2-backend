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
import edu.ai.lms.core.course.adapter.in.response.DeleteChapterResponse;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.ChapterNotFoundException;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.out.DeleteChapterPort;
import edu.ai.lms.core.course.app.port.out.LoadChapterPort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;

@ExtendWith(MockitoExtension.class)
class DeleteChapterServiceUT {

    @Mock
    private DeleteChapterPort deleteChapterPort;

    @Mock
    private LoadChapterPort loadChapterPort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private DeleteChapterService deleteChapterService;

    private UserAuth mockUser;
    private UUID teacherId;
    private UUID courseId;
    private Long chapterNumber;
    private Course mockCourse;
    private Chapter mockChapter;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        chapterNumber = 1L;

        mockUser = UserAuth.builder()
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

        mockChapter = Chapter.builder()
                .chapterNumber(chapterNumber)
                .title("Chapter 1: Introduction")
                .description("Introduction to Java programming")
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully delete a chapter with valid details")
    void testDeleteChapter_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadChapterPort.findByKey(courseId, chapterNumber)).thenReturn(Optional.of(mockChapter));

        // Act
        DeleteChapterResponse response = deleteChapterService.deleteChapter(courseId, chapterNumber);

        // Assert
        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals(chapterNumber, response.getChapterNumber());
        assertEquals("Chapter deleted successfully", response.getMessage());
        assertTrue(response.getSuccess());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadChapterPort, times(1)).findByKey(courseId, chapterNumber);
        verify(deleteChapterPort, times(1)).deleteByKey(courseId, chapterNumber);
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher is not authenticated")
    void testDeleteChapter_WithNonAuthenticatedTeacher_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        TeacherNotFoundException exception = assertThrows(TeacherNotFoundException.class,
                () -> deleteChapterService.deleteChapter(courseId, chapterNumber));

        assertEquals("Teacher not authenticated", exception.getMessage());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, never()).findById(any());
        verify(loadChapterPort, never()).findByKey(any(), any());
        verify(deleteChapterPort, never()).deleteByKey(any(), any());
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testDeleteChapter_WithNonExistentCourse_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        // Act & Assert
        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class,
                () -> deleteChapterService.deleteChapter(courseId, chapterNumber));

        assertTrue(exception.getMessage().contains("Course not found with id:"));

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadChapterPort, never()).findByKey(any(), any());
        verify(deleteChapterPort, never()).deleteByKey(any(), any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher is not authorized for the course")
    void testDeleteChapter_WithUnauthorizedTeacher_ShouldThrowException() {
        // Arrange
        UserAuth anotherUser = UserAuth.builder()
                .id(UUID.randomUUID())
                .role(Roles.TEACHER)
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(anotherUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> deleteChapterService.deleteChapter(courseId, chapterNumber));

        assertEquals("Teacher is not authorized to delete chapters in this course", exception.getMessage());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadChapterPort, never()).findByKey(any(), any());
        verify(deleteChapterPort, never()).deleteByKey(any(), any());
    }

    @Test
    @DisplayName("Should throw ChapterNotFoundException when chapter does not exist")
    void testDeleteChapter_WithNonExistentChapter_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadChapterPort.findByKey(courseId, chapterNumber)).thenReturn(Optional.empty());

        // Act & Assert
        ChapterNotFoundException exception = assertThrows(ChapterNotFoundException.class,
                () -> deleteChapterService.deleteChapter(courseId, chapterNumber));

        assertTrue(exception.getMessage().contains("Chapter not found with number:"));

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(loadChapterPort, times(1)).findByKey(courseId, chapterNumber);
        verify(deleteChapterPort, never()).deleteByKey(any(), any());
    }

    @Test
    @DisplayName("Should call deleteByKey with correct parameters")
    void testDeleteChapter_ShouldCallDeleteByKeyWithCorrectParams() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadChapterPort.findByKey(courseId, chapterNumber)).thenReturn(Optional.of(mockChapter));

        // Act
        deleteChapterService.deleteChapter(courseId, chapterNumber);

        // Assert
        verify(deleteChapterPort, times(1)).deleteByKey(courseId, chapterNumber);
    }

    @Test
    @DisplayName("Should delete chapter even when chapter has no description")
    void testDeleteChapter_WithChapterNoDescription_ShouldSucceed() {
        // Arrange
        Chapter chapterNoDescription = Chapter.builder()
                .chapterNumber(chapterNumber)
                .title("Minimal Chapter")
                .description(null)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockUser));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(loadChapterPort.findByKey(courseId, chapterNumber)).thenReturn(Optional.of(chapterNoDescription));

        // Act
        DeleteChapterResponse response = deleteChapterService.deleteChapter(courseId, chapterNumber);

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());

        verify(deleteChapterPort, times(1)).deleteByKey(courseId, chapterNumber);
    }
}
