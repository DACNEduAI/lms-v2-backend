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

    private Long chapterNumber;
    private UUID courseId;
    private Chapter mockChapter;

    @BeforeEach
    void setUp() {
        chapterNumber = 1L;
        courseId = UUID.randomUUID();

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
    @DisplayName("Should successfully get a chapter with valid course ID and chapter number")
    void testGetChapter_WithValidKey_ShouldSucceed() {
        // Arrange
        when(loadChapterPort.findByKeyWithoutMaterials(courseId, chapterNumber)).thenReturn(Optional.of(mockChapter));

        // Act
        GetChapterResponse response = getChapterService.getChapter(courseId, chapterNumber);

        // Assert
        assertNotNull(response);
        assertEquals(chapterNumber, response.getChapterNumber());
        assertEquals("Chapter 1: Introduction", response.getTitle());
        assertEquals("Introduction to Java programming", response.getDescription());
        assertEquals(courseId, response.getCourseId());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());

        verify(loadChapterPort, times(1)).findByKeyWithoutMaterials(courseId, chapterNumber);
    }

    @Test
    @DisplayName("Should throw ChapterNotFoundException when chapter does not exist")
    void testGetChapter_WithNonExistentChapter_ShouldThrowException() {
        // Arrange
        Long nonExistentChapterNumber = 999L;
        when(loadChapterPort.findByKeyWithoutMaterials(courseId, nonExistentChapterNumber)).thenReturn(Optional.empty());

        // Act & Assert
        ChapterNotFoundException exception = assertThrows(ChapterNotFoundException.class,
                () -> getChapterService.getChapter(courseId, nonExistentChapterNumber));

        assertTrue(exception.getMessage().contains("Chapter not found"));

        verify(loadChapterPort, times(1)).findByKeyWithoutMaterials(courseId, nonExistentChapterNumber);
    }

    @Test
    @DisplayName("Should return correct response fields for chapter without description")
    void testGetChapter_WithNullDescription_ShouldSucceed() {
        // Arrange
        Chapter chapterNoDescription = Chapter.builder()
                .chapterNumber(chapterNumber)
                .title("Minimal Chapter")
                .description(null)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(loadChapterPort.findByKeyWithoutMaterials(courseId, chapterNumber)).thenReturn(Optional.of(chapterNoDescription));

        // Act
        GetChapterResponse response = getChapterService.getChapter(courseId, chapterNumber);

        // Assert
        assertNotNull(response);
        assertEquals(chapterNumber, response.getChapterNumber());
        assertEquals("Minimal Chapter", response.getTitle());
        assertEquals(null, response.getDescription());
        assertEquals(courseId, response.getCourseId());

        verify(loadChapterPort, times(1)).findByKeyWithoutMaterials(courseId, chapterNumber);
    }

    @Test
    @DisplayName("Should map courseId correctly from Chapter domain to response")
    void testGetChapter_ShouldMapCourseIdCorrectly() {
        // Arrange
        UUID specificCourseId = UUID.randomUUID();
        Chapter chapter = Chapter.builder()
                .chapterNumber(chapterNumber)
                .title("Mapped Chapter")
                .description("Testing courseId mapping")
                .courseId(specificCourseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(loadChapterPort.findByKeyWithoutMaterials(specificCourseId, chapterNumber)).thenReturn(Optional.of(chapter));

        // Act
        GetChapterResponse response = getChapterService.getChapter(specificCourseId, chapterNumber);

        // Assert
        assertNotNull(response);
        assertEquals(specificCourseId, response.getCourseId());

        verify(loadChapterPort, times(1)).findByKeyWithoutMaterials(specificCourseId, chapterNumber);
    }

    @Test
    @DisplayName("Should call findByKeyWithoutMaterials, not findByKey")
    void testGetChapter_ShouldUseFindByKeyWithoutMaterials() {
        // Arrange
        when(loadChapterPort.findByKeyWithoutMaterials(courseId, chapterNumber)).thenReturn(Optional.of(mockChapter));

        // Act
        getChapterService.getChapter(courseId, chapterNumber);

        // Assert - verify the correct port method is called
        verify(loadChapterPort, times(1)).findByKeyWithoutMaterials(courseId, chapterNumber);
        verify(loadChapterPort, never()).findByKey(courseId, chapterNumber);
    }
}
