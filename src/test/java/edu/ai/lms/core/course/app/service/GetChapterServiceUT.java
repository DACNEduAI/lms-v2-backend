package edu.ai.lms.core.course.app.service;

import java.time.LocalDateTime;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.course.adapter.in.response.GetChapterResponse;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.exception.ChapterNotFoundException;
import edu.ai.lms.core.course.app.port.out.LoadChapterPort;

@ExtendWith(MockitoExtension.class)
class GetChapterServiceUT {

    @Mock
    private LoadChapterPort loadChapterPort;

    @InjectMocks
    private GetChapterService getChapterService;

    private UUID chapterId;
    private UUID courseId;
    private Chapter mockChapter;

    @BeforeEach
    void setUp() {
        chapterId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        mockChapter = Chapter.builder()
                .id(chapterId)
                .title("Chapter 1: Introduction")
                .description("Introduction to Java programming")
                .orderIndex(1)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully get a chapter with valid chapter ID")
    void testGetChapter_WithValidChapterId_ShouldSucceed() {
        // Arrange
        when(loadChapterPort.findByIdWithoutMaterials(chapterId)).thenReturn(Optional.of(mockChapter));

        // Act
        GetChapterResponse response = getChapterService.getChapter(chapterId);

        // Assert
        assertNotNull(response);
        assertEquals(chapterId, response.getChapterId());
        assertEquals("Chapter 1: Introduction", response.getTitle());
        assertEquals("Introduction to Java programming", response.getDescription());
        assertEquals(1, response.getOrderIndex());
        assertEquals(courseId, response.getCourseId());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());

        verify(loadChapterPort, times(1)).findByIdWithoutMaterials(chapterId);
    }

    @Test
    @DisplayName("Should throw ChapterNotFoundException when chapter does not exist")
    void testGetChapter_WithNonExistentChapter_ShouldThrowException() {
        // Arrange
        UUID nonExistentChapterId = UUID.randomUUID();
        when(loadChapterPort.findByIdWithoutMaterials(nonExistentChapterId)).thenReturn(Optional.empty());

        // Act & Assert
        ChapterNotFoundException exception = assertThrows(ChapterNotFoundException.class,
                () -> getChapterService.getChapter(nonExistentChapterId));

        assertTrue(exception.getMessage().contains("Chapter not found with id:"));

        verify(loadChapterPort, times(1)).findByIdWithoutMaterials(nonExistentChapterId);
    }

    @Test
    @DisplayName("Should return correct response fields for chapter without description")
    void testGetChapter_WithNullDescription_ShouldSucceed() {
        // Arrange
        Chapter chapterNoDescription = Chapter.builder()
                .id(chapterId)
                .title("Minimal Chapter")
                .description(null)
                .orderIndex(1)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(loadChapterPort.findByIdWithoutMaterials(chapterId)).thenReturn(Optional.of(chapterNoDescription));

        // Act
        GetChapterResponse response = getChapterService.getChapter(chapterId);

        // Assert
        assertNotNull(response);
        assertEquals(chapterId, response.getChapterId());
        assertEquals("Minimal Chapter", response.getTitle());
        assertEquals(null, response.getDescription());
        assertEquals(courseId, response.getCourseId());

        verify(loadChapterPort, times(1)).findByIdWithoutMaterials(chapterId);
    }

    @Test
    @DisplayName("Should return correct response fields for chapter without orderIndex")
    void testGetChapter_WithNullOrderIndex_ShouldSucceed() {
        // Arrange
        Chapter chapterNoOrder = Chapter.builder()
                .id(chapterId)
                .title("Unordered Chapter")
                .description("A chapter without an order index")
                .orderIndex(null)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(loadChapterPort.findByIdWithoutMaterials(chapterId)).thenReturn(Optional.of(chapterNoOrder));

        // Act
        GetChapterResponse response = getChapterService.getChapter(chapterId);

        // Assert
        assertNotNull(response);
        assertEquals(chapterId, response.getChapterId());
        assertEquals("Unordered Chapter", response.getTitle());
        assertEquals(null, response.getOrderIndex());
        assertEquals(courseId, response.getCourseId());

        verify(loadChapterPort, times(1)).findByIdWithoutMaterials(chapterId);
    }

    @Test
    @DisplayName("Should map courseId correctly from Chapter domain to response")
    void testGetChapter_ShouldMapCourseIdCorrectly() {
        // Arrange
        UUID specificCourseId = UUID.randomUUID();
        Chapter chapter = Chapter.builder()
                .id(chapterId)
                .title("Mapped Chapter")
                .description("Testing courseId mapping")
                .orderIndex(5)
                .courseId(specificCourseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(loadChapterPort.findByIdWithoutMaterials(chapterId)).thenReturn(Optional.of(chapter));

        // Act
        GetChapterResponse response = getChapterService.getChapter(chapterId);

        // Assert
        assertNotNull(response);
        assertEquals(specificCourseId, response.getCourseId());
        assertEquals(5, response.getOrderIndex());

        verify(loadChapterPort, times(1)).findByIdWithoutMaterials(chapterId);
    }

    @Test
    @DisplayName("Should call findByIdWithoutMaterials, not findById")
    void testGetChapter_ShouldUseFindByIdWithoutMaterials() {
        // Arrange
        when(loadChapterPort.findByIdWithoutMaterials(chapterId)).thenReturn(Optional.of(mockChapter));

        // Act
        getChapterService.getChapter(chapterId);

        // Assert - verify the correct port method is called
        verify(loadChapterPort, times(1)).findByIdWithoutMaterials(chapterId);
        verify(loadChapterPort, never()).findById(chapterId);
    }
}
