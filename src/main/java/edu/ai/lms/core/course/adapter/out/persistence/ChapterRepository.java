package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.ChapterJdbcTable;

/**
 * Package-private repository interface for chapters.
 */
@Repository
interface ChapterRepository extends CrudRepository<ChapterJdbcTable, UUID> {

    @Query("SELECT * FROM chapters WHERE course_id = :courseId ORDER BY order_index")
    List<ChapterJdbcTable> findByCourseId(@Param("courseId") UUID courseId);

    @Modifying
    @Query("DELETE FROM chapters WHERE course_id = :courseId")
    void deleteByCourseId(@Param("courseId") UUID courseId);
}
