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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.material.adapter.in.response.DeleteMaterialResponse;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.domain.Material.MaterialStatus;
import edu.ai.lms.core.material.app.domain.Material.MaterialType;
import edu.ai.lms.core.material.app.exception.MaterialNotFoundException;
import edu.ai.lms.core.material.app.port.out.persistence.DeleteMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.storage.DeleteFilePort;

@ExtendWith(MockitoExtension.class)
class DeleteMaterialServiceUT {

    @Mock
    private GetMaterialPort getMaterialPort;

    @Mock
    private DeleteMaterialPort deleteMaterialPort;

    @Mock
    private DeleteFilePort deleteFilePort;

    @Mock
    private MaterialIndexingService materialIndexingService;

    @InjectMocks
    private DeleteMaterialService deleteMaterialService;

    private UUID courseId;
    private UUID materialId;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        materialId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should delete material from S3 and database when fileKey exists")
    void testDelete_WithFileKey_ShouldDeleteFromS3AndDb() {
        // Arrange
        String fileKey = "courses/" + courseId + "/doc.pdf";
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

        // Act
        DeleteMaterialResponse response = deleteMaterialService.deleteMaterial(courseId, materialId);

        // Assert
        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals(materialId, response.getMaterialId());
        assertEquals("Material deleted successfully", response.getMessage());
        assertTrue(response.getSuccess());

        // Verify S3 deletion happens before DB deletion
        var inOrder = inOrder(deleteFilePort, deleteMaterialPort);
        inOrder.verify(deleteFilePort).deleteFile(fileKey);
        inOrder.verify(deleteMaterialPort).deleteById(materialId);
        verify(materialIndexingService).queueDelete(courseId, materialId);
    }

    @Test
    @DisplayName("Should skip S3 deletion and only delete from DB when fileKey is null")
    void testDelete_NullFileKey_ShouldSkipS3Deletion() {
        // Arrange
        Material material = Material.builder()
                .id(materialId)
                .title("No FileKey Material")
                .type(MaterialType.PDF)
                .fileKey(null)
                .downloadable(false)
                .status(MaterialStatus.PENDING)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(getMaterialPort.findByIdAndCourseId(materialId, courseId))
                .thenReturn(Optional.of(material));

        // Act
        DeleteMaterialResponse response = deleteMaterialService.deleteMaterial(courseId, materialId);

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        verify(deleteFilePort, never()).deleteFile(any());
        verify(deleteMaterialPort, times(1)).deleteById(materialId);
        verify(materialIndexingService).queueDelete(courseId, materialId);
    }

    @Test
    @DisplayName("Should throw RuntimeException when material not found")
    void testDelete_MaterialNotFound_ShouldThrow() {
        // Arrange
        when(getMaterialPort.findByIdAndCourseId(materialId, courseId))
                .thenReturn(Optional.empty());

        // Act & Assert
        MaterialNotFoundException exception = assertThrows(MaterialNotFoundException.class,
                () -> deleteMaterialService.deleteMaterial(courseId, materialId));
        assertTrue(exception.getMessage().contains(materialId.toString()));
        assertTrue(exception.getMessage().contains(courseId.toString()));
        verify(deleteFilePort, never()).deleteFile(any());
        verify(deleteMaterialPort, never()).deleteById(any());
    }
}
