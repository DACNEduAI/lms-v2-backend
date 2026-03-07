package edu.ai.lms.core.material.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.MaterialJdbcTable;

@Repository
interface MaterialRepository extends CrudRepository<MaterialJdbcTable, UUID> {

    // ─── Reads ─────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM materials WHERE id = :id AND course_id = :courseId")
    Optional<MaterialJdbcTable> findByIdAndCourseId(
            @Param("id") UUID id,
            @Param("courseId") UUID courseId);

    @Query("SELECT * FROM materials WHERE file_key = :fileKey")
    Optional<MaterialJdbcTable> findByFileKey(@Param("fileKey") String fileKey);

    @Query("SELECT * FROM materials WHERE course_id = :courseId")
    List<MaterialJdbcTable> findByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT * FROM materials WHERE status IN (:statuses)")
    List<MaterialJdbcTable> findByStatusIn(@Param("statuses") List<String> statuses);

    @Query("SELECT m.* FROM materials m " +
           "INNER JOIN chapter_materials cm ON m.id = cm.material_id " +
           "WHERE cm.chapter_number = :chapterNumber AND cm.course_id = :courseId")
    List<MaterialJdbcTable> findByChapterKey(
            @Param("chapterNumber") Long chapterNumber,
            @Param("courseId") UUID courseId);

    // ─── Junction table operations ─────────────────────────────────────────────

    @Modifying
    @Query("INSERT INTO chapter_materials (material_id, chapter_number, course_id) " +
           "VALUES (:materialId, :chapterNumber, :courseId)")
    void insertChapterMaterial(
            @Param("materialId") UUID materialId,
            @Param("chapterNumber") Long chapterNumber,
            @Param("courseId") UUID courseId);

    // ─── Deletes ───────────────────────────────────────────────────────────────

    @Modifying
    @Query("DELETE FROM chapter_materials WHERE material_id = :materialId")
    void deleteChapterMaterialLinks(@Param("materialId") UUID materialId);

    @Modifying
    @Query("DELETE FROM materials WHERE course_id = :courseId")
    void deleteByCourseId(@Param("courseId") UUID courseId);
}
