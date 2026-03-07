package edu.ai.lms.core.material.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.material.adapter.out.persistence.mapper.MaterialJdbcMapper;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.port.out.persistence.CreateMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.DeleteMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.ModifyMaterialPort;
import edu.ai.lms.database.MaterialJdbcTable;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class MaterialPersistenceAdapter implements CreateMaterialPort, GetMaterialPort, ModifyMaterialPort, DeleteMaterialPort {

    private final MaterialRepository materialRepository;
    private final MaterialJdbcMapper materialJdbcMapper;

    // ─── CreateMaterialPort ────────────────────────────────────────────────────

    @Override
    public Material createForCourse(UUID courseId, Material material) {
        MaterialJdbcTable entity = materialJdbcMapper.toEntity(material, courseId);
        MaterialJdbcTable saved = materialRepository.save(entity);
        return materialJdbcMapper.toDomain(saved);
    }

    @Override
    public Material createForChapter(UUID courseId, Long chapterNumber, Material material) {
        // 1. Save the material row (linked to course via course_id FK)
        MaterialJdbcTable entity = materialJdbcMapper.toEntity(material, courseId);
        MaterialJdbcTable saved = materialRepository.save(entity);

        // 2. Insert the junction row into chapter_materials
        materialRepository.insertChapterMaterial(saved.getId(), chapterNumber, courseId);

        return materialJdbcMapper.toDomain(saved);
    }

    // ─── GetMaterialPort ───────────────────────────────────────────────────────

    @Override
    public Optional<Material> findById(UUID materialId) {
        return materialRepository.findById(materialId)
                .map(materialJdbcMapper::toDomain);
    }

    @Override
    public Optional<Material> findByIdAndCourseId(UUID materialId, UUID courseId) {
        return materialRepository.findByIdAndCourseId(materialId, courseId)
                .map(materialJdbcMapper::toDomain);
    }

    @Override
    public Optional<Material> findByFileKey(String fileKey) {
        return materialRepository.findByFileKey(fileKey)
                .map(materialJdbcMapper::toDomain);
    }

    @Override
    public List<Material> findByCourseId(UUID courseId) {
        return materialRepository.findByCourseId(courseId).stream()
                .map(materialJdbcMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Material> findByChapter(Long chapterNumber, UUID courseId) {
        return materialRepository.findByChapterKey(chapterNumber, courseId).stream()
                .map(materialJdbcMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Material> findByStatuses(List<Material.MaterialStatus> statuses) {
        List<String> statusStrings = statuses.stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return materialRepository.findByStatusIn(statusStrings).stream()
                .map(materialJdbcMapper::toDomain)
                .collect(Collectors.toList());
    }

    // ─── ModifyMaterialPort ────────────────────────────────────────────────────

    @Override
    public Material update(Material material) {
        MaterialJdbcTable entity = materialJdbcMapper.toEntity(material, material.getCourseId());
        MaterialJdbcTable saved = materialRepository.save(entity);
        return materialJdbcMapper.toDomain(saved);
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
