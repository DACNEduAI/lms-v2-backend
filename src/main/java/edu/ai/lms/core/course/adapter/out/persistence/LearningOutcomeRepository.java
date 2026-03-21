package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import edu.ai.lms.core.course.app.domain.LearningOutcome;

/**
 * Package-private repository interface for learning outcomes.
 */
@Mapper
interface LearningOutcomeRepository {

    java.util.Optional<LearningOutcome> findById(@Param("id") UUID id);

    int insert(LearningOutcome entity);

    int update(LearningOutcome entity);

    default LearningOutcome save(LearningOutcome entity) {
        if (entity.getId() == null) {
            entity.setId(java.util.UUID.randomUUID());
            insert(entity);
        } else {
            int updated = update(entity);
            if (updated == 0) {
                insert(entity);
            }
        }
        return findById(entity.getId()).orElseThrow();
    }

    void deleteById(@Param("id") UUID id);

    void deleteAllById(@Param("ids") List<UUID> ids);

    List<LearningOutcome> findByCourseId(@Param("courseId") UUID courseId);

    List<LearningOutcome> findByGroupLearningOutcomeId(@Param("groupId") UUID groupId);

    List<LearningOutcome> findUngroupedByCourseId(@Param("courseId") UUID courseId);

    void updateGroupLearningOutcomeId(@Param("id") UUID id, @Param("courseId") UUID courseId, @Param("groupId") UUID groupId);

    void removeFromGroup(@Param("id") UUID id, @Param("courseId") UUID courseId);

    void deleteByCourseId(@Param("courseId") UUID courseId);
}
