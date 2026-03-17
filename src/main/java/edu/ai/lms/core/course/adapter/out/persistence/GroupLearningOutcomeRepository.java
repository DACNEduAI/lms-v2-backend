package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.GroupLearningOutcomeJdbcTable;

@Repository
interface GroupLearningOutcomeRepository extends CrudRepository<GroupLearningOutcomeJdbcTable, UUID> {

    @Query("SELECT * FROM group_learning_outcomes WHERE course_id = :courseId")
    List<GroupLearningOutcomeJdbcTable> findByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT * FROM group_learning_outcomes WHERE id = :id")
    Optional<GroupLearningOutcomeJdbcTable> findById(@Param("id") UUID id);

    @Modifying
    @Query("DELETE FROM group_learning_outcomes WHERE course_id = :courseId")
    void deleteByCourseId(@Param("courseId") UUID courseId);
}
