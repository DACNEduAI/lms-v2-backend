package edu.ai.lms.core.course.app.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
        when(loadChapterPort.findByKey(courseId, chapterNumber)).thenReturn(Optional.of(mockChapter));

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

        verify(loadChapterPort, times(1)).findByKey(courseId, chapterNumber);
    }

    @Test
    @DisplayName("Should throw ChapterNotFoundException when chapter does not exist")
    void testGetChapter_WithNonExistentChapter_ShouldThrowException() {
        // Arrange
        Long nonExistentChapterNumber = 999L;
        when(loadChapterPort.findByKey(courseId, nonExistentChapterNumber)).thenReturn(Optional.empty());

        // Act & Assert
        ChapterNotFoundException exception = assertThrows(ChapterNotFoundException.class,
                () -> getChapterService.getChapter(courseId, nonExistentChapterNumber));

        assertTrue(exception.getMessage().contains("Chapter not found"));

        verify(loadChapterPort, times(1)).findByKey(courseId, nonExistentChapterNumber);
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

        when(loadChapterPort.findByKey(courseId, chapterNumber)).thenReturn(Optional.of(chapterNoDescription));

        // Act
        GetChapterResponse response = getChapterService.getChapter(courseId, chapterNumber);

        // Assert
        assertNotNull(response);
        assertEquals(chapterNumber, response.getChapterNumber());
        assertEquals("Minimal Chapter", response.getTitle());
        assertEquals(null, response.getDescription());
        assertEquals(courseId, response.getCourseId());

        verify(loadChapterPort, times(1)).findByKey(courseId, chapterNumber);
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

        when(loadChapterPort.findByKey(specificCourseId, chapterNumber)).thenReturn(Optional.of(chapter));

        // Act
        GetChapterResponse response = getChapterService.getChapter(specificCourseId, chapterNumber);

        // Assert
        assertNotNull(response);
        assertEquals(specificCourseId, response.getCourseId());

        verify(loadChapterPort, times(1)).findByKey(specificCourseId, chapterNumber);
    }

    // ==================== getAllChapters Tests ====================

    @Nested
    @DisplayName("getAllChapters tests")
    class GetAllChaptersTests {

        @Test
        @DisplayName("Should return all chapters for a course")
        void testGetAllChapters_WithMultipleChapters_ShouldReturnAll() {
            // Arrange
            Chapter chapter2 = Chapter.builder()
                    .chapterNumber(2L)
                    .title("Chapter 2: Variables")
                    .description("Understanding variables in Java")
                    .courseId(courseId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Chapter chapter3 = Chapter.builder()
                    .chapterNumber(3L)
                    .title("Chapter 3: Control Flow")
                    .description("If-else and switch statements")
                    .courseId(courseId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(loadChapterPort.findAllByCourseId(courseId))
                    .thenReturn(List.of(mockChapter, chapter2, chapter3));

            // Act
            List<GetChapterResponse> responses = getChapterService.getAllChapters(courseId);

            // Assert
            assertNotNull(responses);
            assertEquals(3, responses.size());

            assertEquals(1L, responses.get(0).getChapterNumber());
            assertEquals("Chapter 1: Introduction", responses.get(0).getTitle());
            assertEquals(courseId, responses.get(0).getCourseId());

            assertEquals(2L, responses.get(1).getChapterNumber());
            assertEquals("Chapter 2: Variables", responses.get(1).getTitle());

            assertEquals(3L, responses.get(2).getChapterNumber());
            assertEquals("Chapter 3: Control Flow", responses.get(2).getTitle());

            verify(loadChapterPort, times(1)).findAllByCourseId(courseId);
        }

        @Test
        @DisplayName("Should return empty list when course has no chapters")
        void testGetAllChapters_WithNoChapters_ShouldReturnEmptyList() {
            // Arrange
            when(loadChapterPort.findAllByCourseId(courseId))
                    .thenReturn(Collections.emptyList());

            // Act
            List<GetChapterResponse> responses = getChapterService.getAllChapters(courseId);

            // Assert
            assertNotNull(responses);
            assertTrue(responses.isEmpty());

            verify(loadChapterPort, times(1)).findAllByCourseId(courseId);
        }

        @Test
        @DisplayName("Should return single chapter when course has only one")
        void testGetAllChapters_WithSingleChapter_ShouldReturnOneElement() {
            // Arrange
            when(loadChapterPort.findAllByCourseId(courseId))
                    .thenReturn(List.of(mockChapter));

            // Act
            List<GetChapterResponse> responses = getChapterService.getAllChapters(courseId);

            // Assert
            assertNotNull(responses);
            assertEquals(1, responses.size());
            assertEquals(chapterNumber, responses.get(0).getChapterNumber());
            assertEquals("Chapter 1: Introduction", responses.get(0).getTitle());
            assertEquals("Introduction to Java programming", responses.get(0).getDescription());
            assertEquals(courseId, responses.get(0).getCourseId());
            assertNotNull(responses.get(0).getCreatedAt());
            assertNotNull(responses.get(0).getUpdatedAt());

            verify(loadChapterPort, times(1)).findAllByCourseId(courseId);
        }

        @Test
        @DisplayName("Should map all fields correctly for each chapter")
        void testGetAllChapters_ShouldMapFieldsCorrectly() {
            // Arrange
            LocalDateTime createdAt = LocalDateTime.of(2026, 1, 15, 10, 0);
            LocalDateTime updatedAt = LocalDateTime.of(2026, 2, 20, 14, 30);

            Chapter chapter = Chapter.builder()
                    .chapterNumber(5L)
                    .title("Specific Chapter")
                    .description("Detailed description")
                    .courseId(courseId)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();

            when(loadChapterPort.findAllByCourseId(courseId))
                    .thenReturn(List.of(chapter));

            // Act
            List<GetChapterResponse> responses = getChapterService.getAllChapters(courseId);

            // Assert
            GetChapterResponse response = responses.get(0);
            assertEquals(5L, response.getChapterNumber());
            assertEquals("Specific Chapter", response.getTitle());
            assertEquals("Detailed description", response.getDescription());
            assertEquals(courseId, response.getCourseId());
            assertEquals(createdAt, response.getCreatedAt());
            assertEquals(updatedAt, response.getUpdatedAt());
        }

        @Test
        @DisplayName("Should return empty list for non-existent course (no exception)")
        void testGetAllChapters_WithNonExistentCourse_ShouldReturnEmptyList() {
            // Arrange
            UUID nonExistentCourseId = UUID.randomUUID();
            when(loadChapterPort.findAllByCourseId(nonExistentCourseId))
                    .thenReturn(Collections.emptyList());

            // Act
            List<GetChapterResponse> responses = getChapterService.getAllChapters(nonExistentCourseId);

            // Assert
            assertNotNull(responses);
            assertTrue(responses.isEmpty());

            verify(loadChapterPort, times(1)).findAllByCourseId(nonExistentCourseId);
        }

        @Test
        @DisplayName("Should call findAllByCourseId on LoadChapterPort")
        void testGetAllChapters_ShouldCallCorrectPortMethod() {
            // Arrange
            when(loadChapterPort.findAllByCourseId(courseId))
                    .thenReturn(List.of(mockChapter));

            // Act
            getChapterService.getAllChapters(courseId);

            // Assert
            verify(loadChapterPort, times(1)).findAllByCourseId(courseId);
            verify(loadChapterPort, never()).findByKey(courseId, chapterNumber);
        }
    }
}
