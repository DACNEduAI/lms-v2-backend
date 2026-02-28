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
}
