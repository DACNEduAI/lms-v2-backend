package edu.ai.lms.core.material.app.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.material.adapter.in.response.ConfirmUploadResponse;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.exception.MaterialNotFoundException;
import edu.ai.lms.core.material.app.port.in.usecase.ConfirmUploadUseCase;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.ModifyMaterialPort;
import edu.ai.lms.core.material.app.port.out.storage.VerifyFileUploadedPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfirmUploadService implements ConfirmUploadUseCase {

    private final GetMaterialPort getMaterialPort;
    private final ModifyMaterialPort modifyMaterialPort;
    private final VerifyFileUploadedPort verifyFileUploadedPort;

    @Override
    @Transactional
    public ConfirmUploadResponse confirmUpload(UUID courseId, UUID materialId) {
        Material material = getMaterialPort.findByIdAndCourseId(materialId, courseId)
                .orElseThrow(() -> new MaterialNotFoundException(
                        "Material not found with id: " + materialId + " in course: " + courseId));

        boolean uploaded = verifyFileUploadedPort.isFileUploaded(material.getFileKey());

        if (uploaded) {
            material.setStatus(Material.MaterialStatus.UPLOADED);
        } else {
            material.setStatus(Material.MaterialStatus.FAILED);
        }

        Material updated = modifyMaterialPort.update(material);

        return ConfirmUploadResponse.builder()
                .materialId(updated.getId())
                .courseId(updated.getCourseId())
                .fileKey(updated.getFileKey())
                .status(updated.getStatus().name())
                .uploaded(uploaded)
                .message(uploaded
                        ? "File uploaded successfully. Status updated to UPLOADED."
                        : "File not found in storage. Status updated to FAILED.")
                .build();
    }
}
