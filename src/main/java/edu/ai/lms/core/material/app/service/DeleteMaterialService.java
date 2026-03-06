package edu.ai.lms.core.material.app.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.material.adapter.in.response.DeleteMaterialResponse;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.port.in.usecase.DeleteMaterialUseCase;
import edu.ai.lms.core.material.app.port.out.storage.DeleteFilePort;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.DeleteMaterialPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteMaterialService implements DeleteMaterialUseCase {

    private final GetMaterialPort getMaterialPort;
    private final DeleteMaterialPort deleteMaterialPort;
    private final DeleteFilePort deleteFilePort;

    @Override
    @Transactional
    public DeleteMaterialResponse deleteMaterial(UUID courseId, UUID materialId) {
        // Verify the material exists and belongs to the course
        Material material = getMaterialPort.findByIdAndCourseId(materialId, courseId)
                .orElseThrow(() -> new RuntimeException(
                        "Material not found with id: " + materialId + " in course: " + courseId));

        // Delete file from S3 storage first
        if (material.getFileKey() != null) {
            deleteFilePort.deleteFile(material.getFileKey());
        }

        // Then delete DB record
        deleteMaterialPort.deleteById(materialId);

        return DeleteMaterialResponse.builder()
                .courseId(courseId)
                .materialId(materialId)
                .message("Material deleted successfully")
                .success(true)
                .build();
    }
}
