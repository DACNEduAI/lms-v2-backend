package edu.ai.lms.core.material.app.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import edu.ai.lms.core.material.adapter.in.response.ConfirmUploadResponse;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.domain.Material.MaterialStatus;
import edu.ai.lms.core.material.app.domain.Material.MaterialType;
import edu.ai.lms.core.material.app.exception.MaterialNotFoundException;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.ModifyMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.LoadSignedInUserPort;
import edu.ai.lms.core.material.app.port.out.storage.VerifyFileUploadedPort;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.common.Roles;

@ExtendWith(MockitoExtension.class)
class ConfirmUploadServiceUT {

    @Mock
    private GetMaterialPort getMaterialPort;

    @Mock
    private ModifyMaterialPort modifyMaterialPort;

    @Mock
    private VerifyFileUploadedPort verifyFileUploadedPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ConfirmUploadService confirmUploadService;

    private UUID courseId;
    private UUID materialId;
    private Material material;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        materialId = UUID.randomUUID();

        material = Material.builder()
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

    @Test
    @DisplayName("Should confirm upload and set status to UPLOADED when file exists")
    void testConfirmUpload_WhenFileExists_ShouldSetStatusUploaded() {
        // Arrange
        when(getMaterialPort.findByIdAndCourseId(materialId, courseId)).thenReturn(Optional.of(material));
        when(verifyFileUploadedPort.isFileUploaded(material.getFileKey())).thenReturn(true);
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.empty());

        Material updatedMaterial = Material.builder()
                .id(materialId)
                .title("Lecture Notes")
                .type(MaterialType.PDF)
                .fileKey(material.getFileKey())
                .status(MaterialStatus.UPLOADED)
                .courseId(courseId)
                .createdAt(material.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        when(modifyMaterialPort.update(any(Material.class))).thenReturn(updatedMaterial);

        // Act
        ConfirmUploadResponse response = confirmUploadService.confirmUpload(courseId, materialId);

        // Assert
        assertNotNull(response);
        assertEquals(materialId, response.getMaterialId());
        assertEquals(courseId, response.getCourseId());
        assertEquals("UPLOADED", response.getStatus());
        assertTrue(response.isUploaded());
        assertTrue(response.getMessage().contains("uploaded successfully"));

        verify(getMaterialPort, times(1)).findByIdAndCourseId(materialId, courseId);
        verify(verifyFileUploadedPort, times(1)).isFileUploaded(material.getFileKey());
        verify(modifyMaterialPort, times(1)).update(any(Material.class));
    }

    @Test
    @DisplayName("Should confirm upload and set status to FAILED when file does not exist")
    void testConfirmUpload_WhenFileDoesNotExist_ShouldSetStatusFailed() {
        // Arrange
        when(getMaterialPort.findByIdAndCourseId(materialId, courseId)).thenReturn(Optional.of(material));
        when(verifyFileUploadedPort.isFileUploaded(material.getFileKey())).thenReturn(false);

        Material updatedMaterial = Material.builder()
                .id(materialId)
                .title("Lecture Notes")
                .type(MaterialType.PDF)
                .fileKey(material.getFileKey())
                .status(MaterialStatus.FAILED)
                .courseId(courseId)
                .build();
        when(modifyMaterialPort.update(any(Material.class))).thenReturn(updatedMaterial);

        // Act
        ConfirmUploadResponse response = confirmUploadService.confirmUpload(courseId, materialId);

        // Assert
        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertFalse(response.isUploaded());
        assertTrue(response.getMessage().contains("not found in storage"));

        verify(modifyMaterialPort, times(1)).update(any(Material.class));
    }

    @Test
    @DisplayName("Should throw MaterialNotFoundException when material does not exist")
    void testConfirmUpload_WhenMaterialNotFound_ShouldThrowException() {
        // Arrange
        when(getMaterialPort.findByIdAndCourseId(materialId, courseId)).thenReturn(Optional.empty());

        // Act & Assert
        MaterialNotFoundException exception = assertThrows(MaterialNotFoundException.class,
                () -> confirmUploadService.confirmUpload(courseId, materialId));

        assertTrue(exception.getMessage().contains(materialId.toString()));
        assertTrue(exception.getMessage().contains(courseId.toString()));

        verify(verifyFileUploadedPort, never()).isFileUploaded(any());
        verify(modifyMaterialPort, never()).update(any());
    }
}
