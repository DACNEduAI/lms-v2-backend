package edu.ai.lms.core.material.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
}
