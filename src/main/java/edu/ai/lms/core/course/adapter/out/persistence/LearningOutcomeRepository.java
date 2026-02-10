package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.LearningOutcomeJdbcEntity;

/**
 * Package-private repository interface for learning outcomes.
 */
@Repository
interface LearningOutcomeRepository extends CrudRepository<LearningOutcomeJdbcEntity, UUID> {

    @Query("SELECT * FROM learning_outcomes WHERE course_id = :courseId")
    List<LearningOutcomeJdbcEntity> findByCourseId(@Param("courseId") UUID courseId);

    @Query("DELETE FROM learning_outcomes WHERE course_id = :courseId")
    void deleteByCourseId(@Param("courseId") UUID courseId);
}
