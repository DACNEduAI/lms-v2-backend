package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.LearningOutcomeJdbcTable;

/**
 * Package-private repository interface for learning outcomes.
 */
@Repository
interface LearningOutcomeRepository extends CrudRepository<LearningOutcomeJdbcTable, UUID> {

    @Query("SELECT * FROM learning_outcomes WHERE course_id = :courseId")
    List<LearningOutcomeJdbcTable> findByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT * FROM learning_outcomes WHERE group_learning_outcome_id = :groupId")
    List<LearningOutcomeJdbcTable> findByGroupLearningOutcomeId(@Param("groupId") UUID groupId);

    @Query("SELECT * FROM learning_outcomes WHERE course_id = :courseId AND group_learning_outcome_id IS NULL")
    List<LearningOutcomeJdbcTable> findUngroupedByCourseId(@Param("courseId") UUID courseId);

    @Modifying
    @Query("UPDATE learning_outcomes SET group_learning_outcome_id = :groupId, updated_at = NOW() WHERE id = :id AND course_id = :courseId")
    void updateGroupLearningOutcomeId(@Param("id") UUID id, @Param("courseId") UUID courseId, @Param("groupId") UUID groupId);

    @Modifying
    @Query("UPDATE learning_outcomes SET group_learning_outcome_id = NULL, updated_at = NOW() WHERE id = :id AND course_id = :courseId")
    void removeFromGroup(@Param("id") UUID id, @Param("courseId") UUID courseId);

    @Modifying
    @Query("DELETE FROM learning_outcomes WHERE course_id = :courseId")
    void deleteByCourseId(@Param("courseId") UUID courseId);
}
