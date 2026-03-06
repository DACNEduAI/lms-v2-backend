package edu.ai.lms.core.material.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.material.adapter.in.response.ModifyMaterialResponse;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.port.in.command.ModifyMaterialCommand;
import edu.ai.lms.core.material.app.port.in.usecase.ModifyMaterialUseCase;
import edu.ai.lms.core.material.app.port.out.storage.CreateUploadLinkPort;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.ModifyMaterialPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModifyMaterialService implements ModifyMaterialUseCase {

    private final GetMaterialPort getMaterialPort;
    private final ModifyMaterialPort modifyMaterialPort;
    private final CreateUploadLinkPort createUploadLinkPort;

    @Override
    @Transactional
    public ModifyMaterialResponse modifyMaterial(ModifyMaterialCommand command) {
        Material existing = getMaterialPort.findByIdAndCourseId(command.materialId(), command.courseId())
                .orElseThrow(() -> new RuntimeException(
                        "Material not found with id: " + command.materialId() + " in course: " + command.courseId()));

        Material patch = command.material();
        boolean fileKeyChanged = patch.getFileKey() != null && !patch.getFileKey().equals(existing.getFileKey());

        if (patch.getTitle() != null) existing.setTitle(patch.getTitle());
        if (patch.getType() != null) existing.setType(patch.getType());
        if (patch.getFileKey() != null) existing.setFileKey(patch.getFileKey());
        existing.setDownloadable(patch.isDownloadable());

        // Reset status to PENDING when the file key changes (new upload expected)
        if (fileKeyChanged) {
            existing.setStatus(Material.MaterialStatus.PENDING);
        }

        Material updated = modifyMaterialPort.update(existing);

        // Generate a presigned upload URL when the file key changed
        String presignedUploadUrl = null;
        if (fileKeyChanged && updated.getFileKey() != null) {
            presignedUploadUrl = createUploadLinkPort.createUploadLink(
                    updated.getFileKey(), resolveContentType(updated));
        }

        return ModifyMaterialResponse.builder()
                .id(updated.getId())
                .title(updated.getTitle())
                .type(updated.getType() != null ? updated.getType().name() : null)
                .fileKey(updated.getFileKey())
                .isDownloadable(updated.isDownloadable())
                .status(updated.getStatus() != null ? updated.getStatus().name() : null)
                .presignedUploadUrl(presignedUploadUrl)
                .courseId(updated.getCourseId())
                .updatedAt(updated.getUpdatedAt())
                .build();
    }

    private String resolveContentType(Material m) {
        if (m.getType() == null) return "application/octet-stream";
        return switch (m.getType()) {
            case PDF -> "application/pdf";
            case VIDEO -> "video/mp4";
        };
    }
}
