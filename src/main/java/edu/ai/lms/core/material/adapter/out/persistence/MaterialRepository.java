package edu.ai.lms.core.material.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import edu.ai.lms.core.material.app.domain.Material;

@Mapper
interface MaterialRepository {

        Optional<Material> findById(@Param("id") UUID id);

        int insert(Material material);

        int update(Material material);

        default Material save(Material material) {
        if (material.getId() == null) {
                        material.setId(java.util.UUID.randomUUID());
            insert(material);
        } else {
            int updated = update(material);
            if (updated == 0) {
                insert(material);
            }
        }
        return findById(material.getId()).orElseThrow();
    }

    void deleteById(@Param("id") UUID id);

    // ─── Reads ─────────────────────────────────────────────────────────────────

    Optional<Material> findByIdAndCourseId(
            @Param("id") UUID id,
            @Param("courseId") UUID courseId);

    Optional<Material> findByFileKey(@Param("fileKey") String fileKey);

    List<Material> findByCourseId(@Param("courseId") UUID courseId);

    List<Material> findByStatusIn(@Param("statuses") List<String> statuses);

    List<Material> findByChapterKey(
            @Param("chapterNumber") Long chapterNumber,
            @Param("courseId") UUID courseId);

    // ─── Junction table operations ─────────────────────────────────────────────

    void insertChapterMaterial(
            @Param("materialId") UUID materialId,
            @Param("chapterNumber") Long chapterNumber,
            @Param("courseId") UUID courseId);

    // ─── Deletes ───────────────────────────────────────────────────────────────

    void deleteChapterMaterialLinks(@Param("materialId") UUID materialId);

    void deleteByCourseId(@Param("courseId") UUID courseId);
}
