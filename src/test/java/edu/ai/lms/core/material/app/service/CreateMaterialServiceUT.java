package edu.ai.lms.core.material.app.service;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.material.adapter.in.response.CreateMaterialResponse;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.domain.Material.MaterialStatus;
import edu.ai.lms.core.material.app.domain.Material.MaterialType;
import edu.ai.lms.core.material.app.exception.MaterialAlreadyUploadedException;
import edu.ai.lms.core.material.app.exception.MaterialUploadPendingException;
import edu.ai.lms.core.material.app.port.in.command.CreateMaterialForChapterCommand;
import edu.ai.lms.core.material.app.port.in.command.CreateMaterialForCourseCommand;
import edu.ai.lms.core.material.app.port.out.persistence.CreateMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.storage.CreateUploadLinkPort;

@ExtendWith(MockitoExtension.class)
class CreateMaterialServiceUT {

    @Mock
    private CreateMaterialPort createMaterialPort;

    @Mock
    private GetMaterialPort getMaterialPort;

    @Mock
    private CreateUploadLinkPort createUploadLinkPort;

    @InjectMocks
    private CreateMaterialService createMaterialService;

    private UUID courseId;
    private UUID materialId;
    private Material material;
    private Material savedMaterial;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        materialId = UUID.randomUUID();

        material = Material.builder()
                .title("Lecture Notes")
                .type(MaterialType.PDF)
                .fileKey("courses/" + courseId + "/Lecture_Notes.pdf")
                .downloadable(true)
                .build();

        savedMaterial = Material.builder()
                .id(materialId)
                .title("Lecture Notes")
                .type(MaterialType.PDF)
                .fileKey("courses/" + courseId + "/Lecture_Notes.pdf")
                .downloadable(true)
                .status(MaterialStatus.PENDING)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── createMaterialForCourse ────────────────────────────────────────────────

    @Test
    @DisplayName("Should successfully create a material for a course")
    void testCreateMaterialForCourse_WithValidData_ShouldSucceed() {
        // Arrange
        when(getMaterialPort.findByFileKey(material.getFileKey())).thenReturn(Optional.empty());
        when(createMaterialPort.createForCourse(eq(courseId), any(Material.class))).thenReturn(savedMaterial);
        when(createUploadLinkPort.createUploadLink(eq(savedMaterial.getFileKey()), eq("application/pdf"), isNull()))
                .thenReturn("https://presigned-url.example.com/upload");

        CreateMaterialForCourseCommand command = CreateMaterialForCourseCommand.builder()
                .courseId(courseId)
                .material(material)
                .build();

        // Act
        CreateMaterialResponse response = createMaterialService.createMaterialForCourse(command);

        // Assert
        assertNotNull(response);
        assertEquals(materialId, response.getId());
        assertEquals("Lecture Notes", response.getTitle());
        assertEquals("PDF", response.getType());
        assertEquals("PENDING", response.getStatus());
        assertEquals("https://presigned-url.example.com/upload", response.getPresignedUploadUrl());
        assertEquals(courseId, response.getCourseId());

        verify(getMaterialPort, times(1)).findByFileKey(material.getFileKey());
        verify(createMaterialPort, times(1)).createForCourse(eq(courseId), any(Material.class));
        verify(createUploadLinkPort, times(1)).createUploadLink(savedMaterial.getFileKey(), "application/pdf", null);
    }

    @Test
    @DisplayName("Should throw MaterialAlreadyUploadedException when fileKey exists with UPLOADED status")
    void testCreateMaterialForCourse_WithDuplicateUploadedFileKey_ShouldThrowException() {
        // Arrange
        Material existingMaterial = Material.builder()
                .id(UUID.randomUUID())
                .fileKey(material.getFileKey())
                .status(MaterialStatus.UPLOADED)
                .build();
        when(getMaterialPort.findByFileKey(material.getFileKey())).thenReturn(Optional.of(existingMaterial));

        CreateMaterialForCourseCommand command = CreateMaterialForCourseCommand.builder()
                .courseId(courseId)
                .material(material)
                .build();

        // Act & Assert
        MaterialAlreadyUploadedException exception = assertThrows(MaterialAlreadyUploadedException.class,
                () -> createMaterialService.createMaterialForCourse(command));

        assertTrue(exception.getMessage().contains("already been uploaded"));
        verify(createMaterialPort, never()).createForCourse(any(), any());
        verify(createUploadLinkPort, never()).createUploadLink(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw MaterialUploadPendingException when fileKey exists with PENDING status")
    void testCreateMaterialForCourse_WithDuplicatePendingFileKey_ShouldThrowException() {
        // Arrange
        Material existingMaterial = Material.builder()
                .id(UUID.randomUUID())
                .fileKey(material.getFileKey())
                .status(MaterialStatus.PENDING)
                .build();
        when(getMaterialPort.findByFileKey(material.getFileKey())).thenReturn(Optional.of(existingMaterial));

        CreateMaterialForCourseCommand command = CreateMaterialForCourseCommand.builder()
                .courseId(courseId)
                .material(material)
                .build();

        // Act & Assert
        MaterialUploadPendingException exception = assertThrows(MaterialUploadPendingException.class,
                () -> createMaterialService.createMaterialForCourse(command));

        assertTrue(exception.getMessage().contains("upload status is unknown"));
        verify(createMaterialPort, never()).createForCourse(any(), any());
    }

    @Test
    @DisplayName("Should resolve content type to video/mp4 for VIDEO type")
    void testCreateMaterialForCourse_WithVideoType_ShouldResolveContentType() {
        // Arrange
        Material videoMaterial = Material.builder()
                .title("Lecture Video")
                .type(MaterialType.VIDEO)
                .fileKey("courses/" + courseId + "/Lecture_Video.mp4")
                .downloadable(false)
                .build();

        Material savedVideo = Material.builder()
                .id(materialId)
                .title("Lecture Video")
                .type(MaterialType.VIDEO)
                .fileKey("courses/" + courseId + "/Lecture_Video.mp4")
                .downloadable(false)
                .status(MaterialStatus.PENDING)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .build();

        when(getMaterialPort.findByFileKey(videoMaterial.getFileKey())).thenReturn(Optional.empty());
        when(createMaterialPort.createForCourse(eq(courseId), any(Material.class))).thenReturn(savedVideo);
        when(createUploadLinkPort.createUploadLink(eq(savedVideo.getFileKey()), eq("video/mp4"), isNull()))
                .thenReturn("https://presigned-url.example.com/upload-video");

        CreateMaterialForCourseCommand command = CreateMaterialForCourseCommand.builder()
                .courseId(courseId)
                .material(videoMaterial)
                .build();

        // Act
        CreateMaterialResponse response = createMaterialService.createMaterialForCourse(command);

        // Assert
        assertNotNull(response);
        assertEquals("VIDEO", response.getType());
        verify(createUploadLinkPort, times(1)).createUploadLink(savedVideo.getFileKey(), "video/mp4", null);
    }

    // ── createMaterialForChapter ───────────────────────────────────────────────

    @Test
    @DisplayName("Should successfully create a material for a chapter")
    void testCreateMaterialForChapter_WithValidData_ShouldSucceed() {
        // Arrange
        UUID chapterId = UUID.randomUUID();
        when(getMaterialPort.findByFileKey(material.getFileKey())).thenReturn(Optional.empty());
        when(createMaterialPort.createForChapter(eq(courseId), eq(chapterId), any(Material.class)))
                .thenReturn(savedMaterial);
        when(createUploadLinkPort.createUploadLink(eq(savedMaterial.getFileKey()), eq("application/pdf"), isNull()))
                .thenReturn("https://presigned-url.example.com/upload");

        CreateMaterialForChapterCommand command = CreateMaterialForChapterCommand.builder()
                .courseId(courseId)
                .chapterId(chapterId)
                .material(material)
                .build();

        // Act
        CreateMaterialResponse response = createMaterialService.createMaterialForChapter(command);

        // Assert
        assertNotNull(response);
        assertEquals(materialId, response.getId());
        assertEquals("PENDING", response.getStatus());
        assertNotNull(response.getPresignedUploadUrl());

        verify(createMaterialPort, times(1)).createForChapter(eq(courseId), eq(chapterId), any(Material.class));
    }

    @Test
    @DisplayName("Should throw MaterialAlreadyUploadedException when creating chapter material with duplicate uploaded fileKey")
    void testCreateMaterialForChapter_WithDuplicateUploadedFileKey_ShouldThrowException() {
        // Arrange
        Material existingMaterial = Material.builder()
                .id(UUID.randomUUID())
                .fileKey(material.getFileKey())
                .status(MaterialStatus.UPLOADED)
                .build();
        when(getMaterialPort.findByFileKey(material.getFileKey())).thenReturn(Optional.of(existingMaterial));

        CreateMaterialForChapterCommand command = CreateMaterialForChapterCommand.builder()
                .courseId(courseId)
                .chapterId(UUID.randomUUID())
                .material(material)
                .build();

        // Act & Assert
        assertThrows(MaterialAlreadyUploadedException.class,
                () -> createMaterialService.createMaterialForChapter(command));

        verify(createMaterialPort, never()).createForChapter(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw MaterialUploadPendingException when creating chapter material with duplicate pending fileKey")
    void testCreateMaterialForChapter_WithDuplicatePendingFileKey_ShouldThrowException() {
        // Arrange
        Material existingMaterial = Material.builder()
                .id(UUID.randomUUID())
                .fileKey(material.getFileKey())
                .status(MaterialStatus.PENDING)
                .build();
        when(getMaterialPort.findByFileKey(material.getFileKey())).thenReturn(Optional.of(existingMaterial));

        CreateMaterialForChapterCommand command = CreateMaterialForChapterCommand.builder()
                .courseId(courseId)
                .chapterId(UUID.randomUUID())
                .material(material)
                .build();

        // Act & Assert
        MaterialUploadPendingException exception = assertThrows(MaterialUploadPendingException.class,
                () -> createMaterialService.createMaterialForChapter(command));

        assertTrue(exception.getMessage().contains("upload status is unknown"));
        verify(createMaterialPort, never()).createForChapter(any(), any(), any());
    }

    @Test
    @DisplayName("Should resolve content type to video/mp4 for VIDEO type when creating chapter material")
    void testCreateMaterialForChapter_WithVideoType_ShouldResolveContentType() {
        // Arrange
        UUID chapterId = UUID.randomUUID();
        Material videoMaterial = Material.builder()
                .title("Chapter Video")
                .type(MaterialType.VIDEO)
                .fileKey("courses/" + courseId + "/Chapter_Video.mp4")
                .downloadable(false)
                .build();

        Material savedVideo = Material.builder()
                .id(materialId)
                .title("Chapter Video")
                .type(MaterialType.VIDEO)
                .fileKey("courses/" + courseId + "/Chapter_Video.mp4")
                .downloadable(false)
                .status(MaterialStatus.PENDING)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .build();

        when(getMaterialPort.findByFileKey(videoMaterial.getFileKey())).thenReturn(Optional.empty());
        when(createMaterialPort.createForChapter(eq(courseId), eq(chapterId), any(Material.class)))
                .thenReturn(savedVideo);
        when(createUploadLinkPort.createUploadLink(eq(savedVideo.getFileKey()), eq("video/mp4"), isNull()))
                .thenReturn("https://presigned-url.example.com/upload-video");

        CreateMaterialForChapterCommand command = CreateMaterialForChapterCommand.builder()
                .courseId(courseId)
                .chapterId(chapterId)
                .material(videoMaterial)
                .build();

        // Act
        CreateMaterialResponse response = createMaterialService.createMaterialForChapter(command);

        // Assert
        assertNotNull(response);
        assertEquals("VIDEO", response.getType());
        verify(createUploadLinkPort, times(1)).createUploadLink(savedVideo.getFileKey(), "video/mp4", null);
    }
}
