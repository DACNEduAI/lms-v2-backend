package edu.ai.lms.core.material.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import edu.ai.lms.core.material.adapter.in.response.GetMaterialResponse;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.domain.Material.MaterialStatus;
import edu.ai.lms.core.material.app.domain.Material.MaterialType;
import edu.ai.lms.core.material.app.exception.MaterialNotFoundException;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.storage.CreateDownloadLinkPort;

@ExtendWith(MockitoExtension.class)
class GetMaterialServiceUT {

    @Mock
    private GetMaterialPort getMaterialPort;

    @Mock
    private CreateDownloadLinkPort createDownloadLinkPort;

    @InjectMocks
    private GetMaterialService getMaterialService;

    private UUID courseId;
    private UUID materialId;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        materialId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should return material with presigned download URL when downloadable and has fileKey")
    void testGetMaterial_DownloadableWithFileKey_ShouldReturnPresignedUrl() {
        // Arrange
        String fileKey = "courses/" + courseId + "/doc.pdf";
        String presignedUrl = "https://s3.example.com/presigned-download-url";
        Material material = Material.builder()
                .id(materialId)
                .title("Test Material")
                .type(MaterialType.PDF)
                .fileKey(fileKey)
                .downloadable(true)
                .status(MaterialStatus.UPLOADED)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(getMaterialPort.findByIdAndCourseId(materialId, courseId))
                .thenReturn(Optional.of(material));
        when(createDownloadLinkPort.createDownloadLink(fileKey)).thenReturn(presignedUrl);

        // Act
        GetMaterialResponse response = getMaterialService.getMaterial(courseId, materialId);

        // Assert
        assertNotNull(response);
        assertEquals(materialId, response.getId());
        assertEquals("Test Material", response.getTitle());
        assertEquals("PDF", response.getType());
        assertEquals(fileKey, response.getFileKey());
        assertTrue(response.getIsDownloadable());
        assertEquals("UPLOADED", response.getStatus());
        assertEquals(presignedUrl, response.getPresignedUrl());
        assertEquals(courseId, response.getCourseId());
        verify(createDownloadLinkPort, times(1)).createDownloadLink(fileKey);
    }

    @Test
    @DisplayName("Should return material without presigned URL when not downloadable")
    void testGetMaterial_NotDownloadable_ShouldNotReturnPresignedUrl() {
        // Arrange
        String fileKey = "courses/" + courseId + "/doc.pdf";
        Material material = Material.builder()
                .id(materialId)
                .title("Non-Downloadable Material")
                .type(MaterialType.PDF)
                .fileKey(fileKey)
                .downloadable(false)
                .status(MaterialStatus.UPLOADED)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(getMaterialPort.findByIdAndCourseId(materialId, courseId))
                .thenReturn(Optional.of(material));

        // Act
        GetMaterialResponse response = getMaterialService.getMaterial(courseId, materialId);

        // Assert
        assertNotNull(response);
        assertNull(response.getPresignedUrl());
        assertFalse(response.getIsDownloadable());
        verify(createDownloadLinkPort, never()).createDownloadLink(any());
    }

    @Test
    @DisplayName("Should return material without presigned URL when fileKey is null")
    void testGetMaterial_NullFileKey_ShouldNotReturnPresignedUrl() {
        // Arrange
        Material material = Material.builder()
                .id(materialId)
                .title("No FileKey Material")
                .type(MaterialType.PDF)
                .fileKey(null)
                .downloadable(true)
                .status(MaterialStatus.PENDING)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(getMaterialPort.findByIdAndCourseId(materialId, courseId))
                .thenReturn(Optional.of(material));

        // Act
        GetMaterialResponse response = getMaterialService.getMaterial(courseId, materialId);

        // Assert
        assertNotNull(response);
        assertNull(response.getPresignedUrl());
        verify(createDownloadLinkPort, never()).createDownloadLink(any());
    }

    @Test
    @DisplayName("Should throw RuntimeException when material not found")
    void testGetMaterial_MaterialNotFound_ShouldThrow() {
        // Arrange
        when(getMaterialPort.findByIdAndCourseId(materialId, courseId))
                .thenReturn(Optional.empty());

        // Act & Assert
        MaterialNotFoundException exception = assertThrows(MaterialNotFoundException.class,
                () -> getMaterialService.getMaterial(courseId, materialId));
        assertTrue(exception.getMessage().contains(materialId.toString()));
        assertTrue(exception.getMessage().contains(courseId.toString()));
    }

    // ──────────────────────────────────────────────────────────────────────
    //                  getUploadedMaterials tests
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return only UPLOADED materials for the given course")
    void testGetUploadedMaterials_ShouldReturnOnlyUploadedMaterials() {
        // Arrange
        Material uploaded1 = Material.builder()
                .id(UUID.randomUUID())
                .title("Lecture Notes")
                .type(MaterialType.PDF)
                .fileKey("courses/" + courseId + "/lecture.pdf")
                .downloadable(true)
                .status(MaterialStatus.UPLOADED)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Material uploaded2 = Material.builder()
                .id(UUID.randomUUID())
                .title("Course Video")
                .type(MaterialType.VIDEO)
                .fileKey("courses/" + courseId + "/video.mp4")
                .downloadable(false)
                .status(MaterialStatus.UPLOADED)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Material pending = Material.builder()
                .id(UUID.randomUUID())
                .title("Pending Material")
                .type(MaterialType.PDF)
                .fileKey("courses/" + courseId + "/pending.pdf")
                .downloadable(true)
                .status(MaterialStatus.PENDING)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(getMaterialPort.findByCourseId(courseId))
                .thenReturn(List.of(uploaded1, uploaded2, pending));

        // Act
        List<GetMaterialResponse> results = getMaterialService.getUploadedMaterials(courseId);

        // Assert
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> "UPLOADED".equals(r.getStatus())));
        verify(getMaterialPort, times(1)).findByCourseId(courseId);
        verify(createDownloadLinkPort, never()).createDownloadLink(any());
    }

    @Test
    @DisplayName("Should return empty list when no materials exist for the course")
    void testGetUploadedMaterials_EmptyCourse_ShouldReturnEmptyList() {
        // Arrange
        when(getMaterialPort.findByCourseId(courseId)).thenReturn(List.of());

        // Act
        List<GetMaterialResponse> results = getMaterialService.getUploadedMaterials(courseId);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(createDownloadLinkPort, never()).createDownloadLink(any());
    }

    @Test
    @DisplayName("Should return empty list when all materials are PENDING or FAILED")
    void testGetUploadedMaterials_AllNonUploaded_ShouldReturnEmptyList() {
        // Arrange
        Material pending = Material.builder()
                .id(UUID.randomUUID()).title("Pending")
                .type(MaterialType.PDF).status(MaterialStatus.PENDING)
                .courseId(courseId).downloadable(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        Material failed = Material.builder()
                .id(UUID.randomUUID()).title("Failed")
                .type(MaterialType.PDF).status(MaterialStatus.FAILED)
                .courseId(courseId).downloadable(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(getMaterialPort.findByCourseId(courseId)).thenReturn(List.of(pending, failed));

        // Act
        List<GetMaterialResponse> results = getMaterialService.getUploadedMaterials(courseId);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should not include presigned URL in uploaded materials list")
    void testGetUploadedMaterials_ShouldNotIncludePresignedUrl() {
        // Arrange
        Material material = Material.builder()
                .id(UUID.randomUUID())
                .title("Downloadable PDF")
                .type(MaterialType.PDF)
                .fileKey("courses/" + courseId + "/doc.pdf")
                .downloadable(true)
                .status(MaterialStatus.UPLOADED)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(getMaterialPort.findByCourseId(courseId)).thenReturn(List.of(material));

        // Act
        List<GetMaterialResponse> results = getMaterialService.getUploadedMaterials(courseId);

        // Assert
        assertEquals(1, results.size());
        assertNull(results.get(0).getPresignedUrl(),
                "presignedUrl must be null in the uploaded-materials list response");
        verify(createDownloadLinkPort, never()).createDownloadLink(any());
    }

    @Test
    @DisplayName("Should map all fields correctly in uploaded materials list")
    void testGetUploadedMaterials_ShouldMapFieldsCorrectly() {
        // Arrange
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Material material = Material.builder()
                .id(id)
                .title("Mapped Material")
                .type(MaterialType.VIDEO)
                .fileKey("courses/" + courseId + "/video.mp4")
                .downloadable(false)
                .status(MaterialStatus.UPLOADED)
                .courseId(courseId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(getMaterialPort.findByCourseId(courseId)).thenReturn(List.of(material));

        // Act
        GetMaterialResponse response = getMaterialService.getUploadedMaterials(courseId).get(0);

        // Assert
        assertEquals(id, response.getId());
        assertEquals("Mapped Material", response.getTitle());
        assertEquals("VIDEO", response.getType());
        assertEquals("courses/" + courseId + "/video.mp4", response.getFileKey());
        assertFalse(response.getIsDownloadable());
        assertEquals("UPLOADED", response.getStatus());
        assertEquals(courseId, response.getCourseId());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
        assertNull(response.getPresignedUrl());
    }

    // ──────────────────────────────────────────────────────────────────────
    //                  getChapterMaterials tests
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return all materials for a chapter regardless of status")
    void testGetChapterMaterials_ShouldReturnAllStatuses() {
        // Arrange
        Long chapterNumber = 1L;
        Material uploaded = Material.builder()
                .id(UUID.randomUUID()).title("Uploaded PDF")
                .type(MaterialType.PDF).fileKey("courses/" + courseId + "/doc.pdf")
                .downloadable(true).status(MaterialStatus.UPLOADED)
                .courseId(courseId).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        Material pending = Material.builder()
                .id(UUID.randomUUID()).title("Pending Video")
                .type(MaterialType.VIDEO).fileKey("courses/" + courseId + "/vid.mp4")
                .downloadable(false).status(MaterialStatus.PENDING)
                .courseId(courseId).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(getMaterialPort.findByChapter(chapterNumber, courseId))
                .thenReturn(List.of(uploaded, pending));

        // Act
        List<GetMaterialResponse> results = getMaterialService.getChapterMaterials(courseId, chapterNumber);

        // Assert
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(r -> "UPLOADED".equals(r.getStatus())));
        assertTrue(results.stream().anyMatch(r -> "PENDING".equals(r.getStatus())));
        verify(getMaterialPort, times(1)).findByChapter(chapterNumber, courseId);
        verify(createDownloadLinkPort, never()).createDownloadLink(any());
    }

    @Test
    @DisplayName("Should return empty list when chapter has no materials")
    void testGetChapterMaterials_EmptyChapter_ShouldReturnEmptyList() {
        // Arrange
        Long chapterNumber = 99L;
        when(getMaterialPort.findByChapter(chapterNumber, courseId)).thenReturn(List.of());

        // Act
        List<GetMaterialResponse> results = getMaterialService.getChapterMaterials(courseId, chapterNumber);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(createDownloadLinkPort, never()).createDownloadLink(any());
    }

    @Test
    @DisplayName("Should not include presigned URL in chapter materials list")
    void testGetChapterMaterials_ShouldNotIncludePresignedUrl() {
        // Arrange
        Long chapterNumber = 2L;
        Material material = Material.builder()
                .id(UUID.randomUUID()).title("Downloadable Lecture")
                .type(MaterialType.PDF).fileKey("courses/" + courseId + "/lecture.pdf")
                .downloadable(true).status(MaterialStatus.UPLOADED)
                .courseId(courseId).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(getMaterialPort.findByChapter(chapterNumber, courseId)).thenReturn(List.of(material));

        // Act
        List<GetMaterialResponse> results = getMaterialService.getChapterMaterials(courseId, chapterNumber);

        // Assert
        assertEquals(1, results.size());
        assertNull(results.get(0).getPresignedUrl(),
                "presignedUrl must be null in the chapter materials list response");
        verify(createDownloadLinkPort, never()).createDownloadLink(any());
    }

    @Test
    @DisplayName("Should map all fields correctly in chapter materials list")
    void testGetChapterMaterials_ShouldMapFieldsCorrectly() {
        // Arrange
        Long chapterNumber = 3L;
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Material material = Material.builder()
                .id(id).title("Chapter Resource")
                .type(MaterialType.PDF).fileKey("courses/" + courseId + "/res.pdf")
                .downloadable(true).status(MaterialStatus.UPLOADED)
                .courseId(courseId).createdAt(now).updatedAt(now)
                .build();

        when(getMaterialPort.findByChapter(chapterNumber, courseId)).thenReturn(List.of(material));

        // Act
        GetMaterialResponse response = getMaterialService.getChapterMaterials(courseId, chapterNumber).get(0);

        // Assert
        assertEquals(id, response.getId());
        assertEquals("Chapter Resource", response.getTitle());
        assertEquals("PDF", response.getType());
        assertEquals("courses/" + courseId + "/res.pdf", response.getFileKey());
        assertTrue(response.getIsDownloadable());
        assertEquals("UPLOADED", response.getStatus());
        assertEquals(courseId, response.getCourseId());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
        assertNull(response.getPresignedUrl());
    }
}
