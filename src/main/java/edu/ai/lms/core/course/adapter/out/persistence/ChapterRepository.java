package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.ChapterJdbcEntity;

/**
 * Package-private repository interface for chapters.
 */
@Repository
interface ChapterRepository extends CrudRepository<ChapterJdbcEntity, UUID> {

    @Query("SELECT * FROM chapters WHERE course_id = :courseId ORDER BY order_index")
    List<ChapterJdbcEntity> findByCourseId(@Param("courseId") UUID courseId);

    @Query("DELETE FROM chapters WHERE course_id = :courseId")
    void deleteByCourseId(@Param("courseId") UUID courseId);
}
