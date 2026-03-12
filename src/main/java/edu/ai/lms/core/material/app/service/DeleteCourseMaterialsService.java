package edu.ai.lms.core.material.app.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.material.adapter.in.internal.DeleteCourseMaterialsApi;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.port.out.persistence.DeleteMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.storage.DeleteFilePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteCourseMaterialsService implements DeleteCourseMaterialsApi {

    private final GetMaterialPort getMaterialPort;
    private final DeleteMaterialPort deleteMaterialPort;
    private final DeleteFilePort deleteFilePort;

    @Override
    @Transactional
    public void deleteAllMaterialsByCourseId(UUID courseId) {
        // 1. Load all materials for the course to get their file keys
        List<Material> materials = getMaterialPort.findByCourseId(courseId);

        // 2. Delete S3 files first
        for (Material material : materials) {
            if (material.getFileKey() != null) {
                deleteFilePort.deleteFile(material.getFileKey());
            }
        }

        // 3. Delete all material DB records for this course
        deleteMaterialPort.deleteByCourseId(courseId);
    }
}
