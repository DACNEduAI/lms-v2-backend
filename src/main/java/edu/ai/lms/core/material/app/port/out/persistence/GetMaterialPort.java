package edu.ai.lms.core.material.app.port.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.material.app.domain.Material;

public interface GetMaterialPort {

    Optional<Material> findById(UUID materialId);

    Optional<Material> findByIdAndCourseId(UUID materialId, UUID courseId);

    Optional<Material> findByFileKey(String fileKey);

    List<Material> findByCourseId(UUID courseId);

    List<Material> findByChapter(UUID chapterId, UUID courseId);

    List<Material> findByStatuses(List<Material.MaterialStatus> statuses);
}
