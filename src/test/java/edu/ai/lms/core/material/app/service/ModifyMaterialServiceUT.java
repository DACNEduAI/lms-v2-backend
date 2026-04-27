package edu.ai.lms.core.material.app.service;

import java.time.LocalDateTime;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.material.adapter.in.response.ModifyMaterialResponse;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.domain.Material.MaterialStatus;
import edu.ai.lms.core.material.app.domain.Material.MaterialType;
import edu.ai.lms.core.material.app.port.in.command.ModifyMaterialCommand;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.ModifyMaterialPort;
import edu.ai.lms.core.material.app.port.out.storage.CreateUploadLinkPort;

@ExtendWith(MockitoExtension.class)
class ModifyMaterialServiceUT {

    @Mock
    private GetMaterialPort getMaterialPort;

    @Mock
    private ModifyMaterialPort modifyMaterialPort;

    @Mock
    private CreateUploadLinkPort createUploadLinkPort;

    @InjectMocks
    private ModifyMaterialService modifyMaterialService;

    private UUID courseId;
    private UUID materialId;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        materialId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should modify material with new fileKey and return presigned upload URL")
    void testModify_FileKeyChanged_ShouldReturnPresignedUrl() {
        // Arrange
        String oldFileKey = "courses/" + courseId + "/old.pdf";
        String newFileKey = "courses/" + courseId + "/new.pdf";
        String presignedUploadUrl = "https://s3.example.com/presigned-upload-url";

        Material existing = Material.builder()
                .id(materialId)
                .title("Old Title")
                .type(MaterialType.PDF)
                .fileKey(oldFileKey)
                .downloadable(true)
                .status(MaterialStatus.UPLOADED)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Material patch = Material.builder()
                .title("New Title")
                .type(MaterialType.PDF)
                .fileKey(newFileKey)
                .downloadable(false)
                .build();

        ModifyMaterialCommand command = ModifyMaterialCommand.builder()
                .courseId(courseId)
                .materialId(materialId)
                .material(patch)
                .build();

        when(getMaterialPort.findByIdAndCourseId(materialId, courseId))
                .thenReturn(Optional.of(existing));
        when(modifyMaterialPort.update(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(createUploadLinkPort.createUploadLink(eq(newFileKey), eq("application/pdf"), isNull()))
                .thenReturn(presignedUploadUrl);

        // Act
        ModifyMaterialResponse response = modifyMaterialService.modifyMaterial(command);

        // Assert
        assertNotNull(response);
        assertEquals("New Title", response.getTitle());
        assertEquals(newFileKey, response.getFileKey());
        assertEquals("PENDING", response.getStatus()); // reset to PENDING
        assertFalse(response.getIsDownloadable());
        assertEquals(presignedUploadUrl, response.getPresignedUploadUrl());
        verify(createUploadLinkPort, times(1)).createUploadLink(newFileKey, "application/pdf", null);
    }

    @Test
    @DisplayName("Should modify material without presigned URL when fileKey unchanged")
    void testModify_FileKeyUnchanged_ShouldNotReturnPresignedUrl() {
        // Arrange
        String fileKey = "courses/" + courseId + "/doc.pdf";

        Material existing = Material.builder()
                .id(materialId)
                .title("Old Title")
                .type(MaterialType.PDF)
                .fileKey(fileKey)
                .downloadable(true)
                .status(MaterialStatus.UPLOADED)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Material patch = Material.builder()
                .title("Updated Title")
                .fileKey(fileKey) // same key
                .downloadable(true)
                .build();

        ModifyMaterialCommand command = ModifyMaterialCommand.builder()
                .courseId(courseId)
                .materialId(materialId)
                .material(patch)
                .build();

        when(getMaterialPort.findByIdAndCourseId(materialId, courseId))
                .thenReturn(Optional.of(existing));
        when(modifyMaterialPort.update(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ModifyMaterialResponse response = modifyMaterialService.modifyMaterial(command);

        // Assert
        assertNotNull(response);
        assertEquals("Updated Title", response.getTitle());
        assertEquals("UPLOADED", response.getStatus()); // status not reset
        assertNull(response.getPresignedUploadUrl());
        verify(createUploadLinkPort, never()).createUploadLink(any(), any(), any());
    }

    @Test
    @DisplayName("Should modify material with VIDEO type and resolve content type to video/mp4")
    void testModify_VideoType_ShouldResolveContentType() {
        // Arrange
        String oldFileKey = "courses/" + courseId + "/old.mp4";
        String newFileKey = "courses/" + courseId + "/new.mp4";
        String presignedUploadUrl = "https://s3.example.com/presigned-upload-url";

        Material existing = Material.builder()
                .id(materialId)
                .title("Old Video")
                .type(MaterialType.VIDEO)
                .fileKey(oldFileKey)
                .downloadable(true)
                .status(MaterialStatus.UPLOADED)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Material patch = Material.builder()
                .title("New Video")
                .type(MaterialType.VIDEO)
                .fileKey(newFileKey)
                .downloadable(true)
                .build();

        ModifyMaterialCommand command = ModifyMaterialCommand.builder()
                .courseId(courseId)
                .materialId(materialId)
                .material(patch)
                .build();

        when(getMaterialPort.findByIdAndCourseId(materialId, courseId))
                .thenReturn(Optional.of(existing));
        when(modifyMaterialPort.update(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(createUploadLinkPort.createUploadLink(eq(newFileKey), eq("video/mp4"), isNull()))
                .thenReturn(presignedUploadUrl);

        // Act
        ModifyMaterialResponse response = modifyMaterialService.modifyMaterial(command);

        // Assert
        assertNotNull(response);
        assertEquals(presignedUploadUrl, response.getPresignedUploadUrl());
        verify(createUploadLinkPort, times(1)).createUploadLink(newFileKey, "video/mp4", null);
    }

    @Test
    @DisplayName("Should throw RuntimeException when material not found")
    void testModify_MaterialNotFound_ShouldThrow() {
        // Arrange
        Material patch = Material.builder().title("Updated").build();
        ModifyMaterialCommand command = ModifyMaterialCommand.builder()
                .courseId(courseId)
                .materialId(materialId)
                .material(patch)
                .build();

        when(getMaterialPort.findByIdAndCourseId(materialId, courseId))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> modifyMaterialService.modifyMaterial(command));
        assertTrue(exception.getMessage().contains(materialId.toString()));
        assertTrue(exception.getMessage().contains(courseId.toString()));
        verify(modifyMaterialPort, never()).update(any());
        verify(createUploadLinkPort, never()).createUploadLink(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle null patch fileKey without resetting status")
    void testModify_NullPatchFileKey_ShouldNotResetStatus() {
        // Arrange
        String fileKey = "courses/" + courseId + "/doc.pdf";

        Material existing = Material.builder()
                .id(materialId)
                .title("Title")
                .type(MaterialType.PDF)
                .fileKey(fileKey)
                .downloadable(true)
                .status(MaterialStatus.UPLOADED)
                .courseId(courseId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Material patch = Material.builder()
                .title("Updated Title")
                .fileKey(null) // null fileKey
                .downloadable(false)
                .build();

        ModifyMaterialCommand command = ModifyMaterialCommand.builder()
                .courseId(courseId)
                .materialId(materialId)
                .material(patch)
                .build();

        when(getMaterialPort.findByIdAndCourseId(materialId, courseId))
                .thenReturn(Optional.of(existing));
        when(modifyMaterialPort.update(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ModifyMaterialResponse response = modifyMaterialService.modifyMaterial(command);

        // Assert
        assertNotNull(response);
        assertEquals("UPLOADED", response.getStatus()); // status stays
        assertNull(response.getPresignedUploadUrl());
        verify(createUploadLinkPort, never()).createUploadLink(any(), any(), any());
    }
}
