package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.ChapterJdbcTable;

/**
 * Package-private repository interface for chapters.
 * Chapter is a weak entity with composite primary key (chapter_number, course_id).
 * We use Long as the ID type (chapter_number) but handle updates via custom queries
 * because the actual primary key is composite and includes course_id.
 */
@Repository
interface ChapterRepository extends CrudRepository<ChapterJdbcTable, Long> {

    @Query("SELECT * FROM chapters WHERE course_id = :courseId ORDER BY chapter_number")
    List<ChapterJdbcTable> findByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT * FROM chapters WHERE chapter_number = :chapterNumber AND course_id = :courseId")
    Optional<ChapterJdbcTable> findByChapterNumberAndCourseId(
            @Param("chapterNumber") Long chapterNumber,
            @Param("courseId") UUID courseId);

    @Modifying
    @Query("DELETE FROM chapters WHERE course_id = :courseId")
    void deleteByCourseId(@Param("courseId") UUID courseId);

    @Modifying
    @Query("DELETE FROM chapters WHERE chapter_number = :chapterNumber AND course_id = :courseId")
    void deleteByChapterNumberAndCourseId(
            @Param("chapterNumber") Long chapterNumber,
            @Param("courseId") UUID courseId);

    /**
     * Custom INSERT that returns the auto-generated chapter_number.
     * Used for creating new chapters since chapter_number is GENERATED ALWAYS AS IDENTITY.
     */
    @Query("INSERT INTO chapters (course_id, title, description, created_at, updated_at) " +
           "VALUES (:courseId, :title, :description, NOW(), NOW()) " +
           "RETURNING chapter_number")
    Long insertAndReturnChapterNumber(
            @Param("courseId") UUID courseId,
            @Param("title") String title,
            @Param("description") String description);

    /**
     * Custom UPDATE by composite key (chapter_number, course_id).
     * Standard save() doesn't work properly with composite keys that have auto-generated parts.
     */
    @Modifying
    @Query("UPDATE chapters SET title = :title, description = :description, updated_at = NOW() " +
           "WHERE chapter_number = :chapterNumber AND course_id = :courseId")
    void updateByCompositeKey(
            @Param("chapterNumber") Long chapterNumber,
            @Param("courseId") UUID courseId,
            @Param("title") String title,
            @Param("description") String description);
}
