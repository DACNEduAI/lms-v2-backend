package edu.ai.lms.core.material.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.domain.Material.MaterialStatus;
import edu.ai.lms.core.material.app.domain.Material.MaterialType;
import edu.ai.lms.core.material.app.port.out.persistence.DeleteMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.storage.VerifyFileUploadedPort;

@ExtendWith(MockitoExtension.class)
class CleanupStaleMaterialsServiceUT {

    @Mock
    private GetMaterialPort getMaterialPort;

    @Mock
    private DeleteMaterialPort deleteMaterialPort;

    @Mock
    private VerifyFileUploadedPort verifyFileUploadedPort;

    @InjectMocks
    private CleanupStaleMaterialsService cleanupStaleMaterialsService;

    private UUID courseId;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should delete FAILED material when file does not exist in storage")
    void testCleanup_FailedMaterialNotInStorage_ShouldDelete() {
        // Arrange
        UUID materialId = UUID.randomUUID();
        Material failedMaterial = Material.builder()
                .id(materialId)
                .type(MaterialType.PDF)
                .fileKey("courses/" + courseId + "/doc.pdf")
                .status(MaterialStatus.FAILED)
                .courseId(courseId)
                .updatedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(getMaterialPort.findByStatuses(List.of(MaterialStatus.PENDING, MaterialStatus.FAILED)))
                .thenReturn(List.of(failedMaterial));
        when(verifyFileUploadedPort.isFileUploaded(failedMaterial.getFileKey())).thenReturn(false);

        // Act
        int deletedCount = cleanupStaleMaterialsService.cleanupStaleMaterials();

        // Assert
        assertEquals(1, deletedCount);
        verify(deleteMaterialPort, times(1)).deleteById(materialId);
    }

    @Test
    @DisplayName("Should not delete FAILED material when file exists in storage")
    void testCleanup_FailedMaterialInStorage_ShouldNotDelete() {
        // Arrange
        UUID materialId = UUID.randomUUID();
        Material failedMaterial = Material.builder()
                .id(materialId)
                .type(MaterialType.PDF)
                .fileKey("courses/" + courseId + "/doc.pdf")
                .status(MaterialStatus.FAILED)
                .courseId(courseId)
                .updatedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(getMaterialPort.findByStatuses(List.of(MaterialStatus.PENDING, MaterialStatus.FAILED)))
                .thenReturn(List.of(failedMaterial));
        when(verifyFileUploadedPort.isFileUploaded(failedMaterial.getFileKey())).thenReturn(true);

        // Act
        int deletedCount = cleanupStaleMaterialsService.cleanupStaleMaterials();

        // Assert
        assertEquals(0, deletedCount);
        verify(deleteMaterialPort, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should skip PENDING PDF material within 5-minute timeout window")
    void testCleanup_PendingPdfWithinTimeout_ShouldSkip() {
        // Arrange
        UUID materialId = UUID.randomUUID();
        Material pendingPdf = Material.builder()
                .id(materialId)
                .type(MaterialType.PDF)
                .fileKey("courses/" + courseId + "/doc.pdf")
                .status(MaterialStatus.PENDING)
                .courseId(courseId)
                .updatedAt(LocalDateTime.now().minusMinutes(3)) // within 5 min threshold
                .build();

        when(getMaterialPort.findByStatuses(List.of(MaterialStatus.PENDING, MaterialStatus.FAILED)))
                .thenReturn(List.of(pendingPdf));

        // Act
        int deletedCount = cleanupStaleMaterialsService.cleanupStaleMaterials();

        // Assert
        assertEquals(0, deletedCount);
        verify(verifyFileUploadedPort, never()).isFileUploaded(any());
        verify(deleteMaterialPort, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should check PENDING PDF material that exceeded 5-minute timeout")
    void testCleanup_PendingPdfExceededTimeout_ShouldCheckAndDelete() {
        // Arrange
        UUID materialId = UUID.randomUUID();
        Material pendingPdf = Material.builder()
                .id(materialId)
                .type(MaterialType.PDF)
                .fileKey("courses/" + courseId + "/doc.pdf")
                .status(MaterialStatus.PENDING)
                .courseId(courseId)
                .updatedAt(LocalDateTime.now().minusMinutes(6)) // exceeded 5 min
                .build();

        when(getMaterialPort.findByStatuses(List.of(MaterialStatus.PENDING, MaterialStatus.FAILED)))
                .thenReturn(List.of(pendingPdf));
        when(verifyFileUploadedPort.isFileUploaded(pendingPdf.getFileKey())).thenReturn(false);

        // Act
        int deletedCount = cleanupStaleMaterialsService.cleanupStaleMaterials();

        // Assert
        assertEquals(1, deletedCount);
        verify(deleteMaterialPort, times(1)).deleteById(materialId);
    }

    @Test
    @DisplayName("Should skip PENDING VIDEO material within 20-minute timeout window")
    void testCleanup_PendingVideoWithinTimeout_ShouldSkip() {
        // Arrange
        UUID materialId = UUID.randomUUID();
        Material pendingVideo = Material.builder()
                .id(materialId)
                .type(MaterialType.VIDEO)
                .fileKey("courses/" + courseId + "/video.mp4")
                .status(MaterialStatus.PENDING)
                .courseId(courseId)
                .updatedAt(LocalDateTime.now().minusMinutes(15)) // within 20 min threshold
                .build();

        when(getMaterialPort.findByStatuses(List.of(MaterialStatus.PENDING, MaterialStatus.FAILED)))
                .thenReturn(List.of(pendingVideo));

        // Act
        int deletedCount = cleanupStaleMaterialsService.cleanupStaleMaterials();

        // Assert
        assertEquals(0, deletedCount);
        verify(verifyFileUploadedPort, never()).isFileUploaded(any());
    }

    @Test
    @DisplayName("Should check PENDING VIDEO material that exceeded 20-minute timeout")
    void testCleanup_PendingVideoExceededTimeout_ShouldCheckAndDelete() {
        // Arrange
        UUID materialId = UUID.randomUUID();
        Material pendingVideo = Material.builder()
                .id(materialId)
                .type(MaterialType.VIDEO)
                .fileKey("courses/" + courseId + "/video.mp4")
                .status(MaterialStatus.PENDING)
                .courseId(courseId)
                .updatedAt(LocalDateTime.now().minusMinutes(25)) // exceeded 20 min
                .build();

        when(getMaterialPort.findByStatuses(List.of(MaterialStatus.PENDING, MaterialStatus.FAILED)))
                .thenReturn(List.of(pendingVideo));
        when(verifyFileUploadedPort.isFileUploaded(pendingVideo.getFileKey())).thenReturn(false);

        // Act
        int deletedCount = cleanupStaleMaterialsService.cleanupStaleMaterials();

        // Assert
        assertEquals(1, deletedCount);
        verify(deleteMaterialPort, times(1)).deleteById(materialId);
    }

    @Test
    @DisplayName("Should return 0 when there are no stale materials")
    void testCleanup_NoStaleMaterials_ShouldReturnZero() {
        // Arrange
        when(getMaterialPort.findByStatuses(List.of(MaterialStatus.PENDING, MaterialStatus.FAILED)))
                .thenReturn(List.of());

        // Act
        int deletedCount = cleanupStaleMaterialsService.cleanupStaleMaterials();

        // Assert
        assertEquals(0, deletedCount);
        verify(verifyFileUploadedPort, never()).isFileUploaded(any());
        verify(deleteMaterialPort, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should handle mixed materials correctly in a single cleanup run")
    void testCleanup_MixedMaterials_ShouldHandleCorrectly() {
        // Arrange
        UUID failedId = UUID.randomUUID();
        UUID pendingRecentId = UUID.randomUUID();
        UUID pendingOldId = UUID.randomUUID();

        Material failedMaterial = Material.builder()
                .id(failedId)
                .type(MaterialType.PDF)
                .fileKey("courses/" + courseId + "/failed.pdf")
                .status(MaterialStatus.FAILED)
                .courseId(courseId)
                .updatedAt(LocalDateTime.now().minusMinutes(30))
                .build();

        Material pendingRecentMaterial = Material.builder()
                .id(pendingRecentId)
                .type(MaterialType.PDF)
                .fileKey("courses/" + courseId + "/recent.pdf")
                .status(MaterialStatus.PENDING)
                .courseId(courseId)
                .updatedAt(LocalDateTime.now().minusMinutes(2)) // within threshold
                .build();

        Material pendingOldMaterial = Material.builder()
                .id(pendingOldId)
                .type(MaterialType.PDF)
                .fileKey("courses/" + courseId + "/old.pdf")
                .status(MaterialStatus.PENDING)
                .courseId(courseId)
                .updatedAt(LocalDateTime.now().minusMinutes(10)) // exceeded threshold
                .build();

        when(getMaterialPort.findByStatuses(List.of(MaterialStatus.PENDING, MaterialStatus.FAILED)))
                .thenReturn(List.of(failedMaterial, pendingRecentMaterial, pendingOldMaterial));
        when(verifyFileUploadedPort.isFileUploaded(failedMaterial.getFileKey())).thenReturn(false);
        when(verifyFileUploadedPort.isFileUploaded(pendingOldMaterial.getFileKey())).thenReturn(false);

        // Act
        int deletedCount = cleanupStaleMaterialsService.cleanupStaleMaterials();

        // Assert
        assertEquals(2, deletedCount);
        verify(deleteMaterialPort, times(1)).deleteById(failedId);
        verify(deleteMaterialPort, times(1)).deleteById(pendingOldId);
        verify(deleteMaterialPort, never()).deleteById(pendingRecentId);
        // Recent pending should never be checked in storage
        verify(verifyFileUploadedPort, never()).isFileUploaded(pendingRecentMaterial.getFileKey());
    }
}
