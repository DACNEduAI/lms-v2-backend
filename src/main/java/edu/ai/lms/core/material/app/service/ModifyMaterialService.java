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

        if (fileKeyChanged) {
            throw new IllegalStateException(
                    "Material file keys are immutable. Create a new material to upload replacement content.");
        }

        if (patch.getTitle() != null) existing.setTitle(patch.getTitle());
        if (patch.getType() != null) existing.setType(patch.getType());
        existing.setDownloadable(patch.isDownloadable());

        Material updated = modifyMaterialPort.update(existing);

        String presignedUploadUrl = null;

        return ModifyMaterialResponse.builder()
                .id(updated.getId())
                .title(updated.getTitle())
                .originalFileName(updated.getOriginalFileName())
                .type(updated.getType() != null ? updated.getType().name() : null)
                .fileKey(updated.getFileKey())
                .isDownloadable(updated.isDownloadable())
                .status(updated.getStatus() != null ? updated.getStatus().name() : null)
                .ingestionStatus(updated.getIngestionStatus() != null ? updated.getIngestionStatus().name() : null)
                .presignedUploadUrl(presignedUploadUrl)
                .courseId(updated.getCourseId())
                .updatedAt(updated.getUpdatedAt())
                .build();
    }
}
