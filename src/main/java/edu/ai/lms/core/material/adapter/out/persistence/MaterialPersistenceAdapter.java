package edu.ai.lms.core.material.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.port.out.persistence.CreateMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.DeleteMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.ModifyMaterialPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class MaterialPersistenceAdapter implements CreateMaterialPort, GetMaterialPort, ModifyMaterialPort, DeleteMaterialPort {

    private final MaterialRepository materialRepository;

    // ─── CreateMaterialPort ────────────────────────────────────────────────────

    @Override
    public Material createForCourse(UUID courseId, Material material) {
        if (material.getStatus() == null) {
            material.setStatus(Material.MaterialStatus.PENDING);
        }
        return materialRepository.save(material);
    }

    @Override
    public Material createForChapter(UUID courseId, UUID chapterId, Material material) {
        if (material.getStatus() == null) {
            material.setStatus(Material.MaterialStatus.PENDING);
        }
        Material saved = materialRepository.save(material);

        // 2. Insert the junction row into chapter_materials
        materialRepository.insertChapterMaterial(saved.getId(), chapterId);

        return saved;
    }

    // ─── GetMaterialPort ───────────────────────────────────────────────────────

    @Override
    public Optional<Material> findById(UUID materialId) {
        return materialRepository.findById(materialId);
    }

    @Override
    public Optional<Material> findByIdAndCourseId(UUID materialId, UUID courseId) {
        return materialRepository.findByIdAndCourseId(materialId, courseId);
    }

    @Override
    public Optional<Material> findByFileKey(String fileKey) {
        return materialRepository.findByFileKey(fileKey);
    }

    @Override
    public List<Material> findByCourseId(UUID courseId) {
        return materialRepository.findByCourseId(courseId).stream().collect(Collectors.toList());
    }

    @Override
    public List<Material> findByChapter(UUID chapterId, UUID courseId) {
        return materialRepository.findByChapterKey(chapterId, courseId).stream().collect(Collectors.toList());
    }

    @Override
    public List<Material> findByStatuses(List<Material.MaterialStatus> statuses) {
        List<String> statusStrings = statuses.stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return materialRepository.findByStatusIn(statusStrings).stream().collect(Collectors.toList());
    }

    @Override
    public List<UUID> getStudentIdsByCourseId(UUID courseId) {
        return materialRepository.findStudentIdsByCourseId(courseId);
    }

    // ─── ModifyMaterialPort ────────────────────────────────────────────────────

    @Override
    public Material update(Material material) {
        return materialRepository.save(material);
    }

    // ─── DeleteMaterialPort ────────────────────────────────────────────────────

    @Override
    public void deleteById(UUID materialId) {
        // Remove junction rows first, then the material itself
        materialRepository.deleteChapterMaterialLinks(materialId);
        materialRepository.deleteById(materialId);
    }

    @Override
    public void deleteByCourseId(UUID courseId) {
        materialRepository.deleteByCourseId(courseId);
    }
}
