package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;

@Mapper
interface GroupLearningOutcomeRepository {

    int insert(GroupLearningOutcome entity);

    int update(GroupLearningOutcome entity);

    default GroupLearningOutcome save(GroupLearningOutcome entity) {
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(1);
        }
        if (entity.getCode() == null) {
            entity.setCode("L.O." + entity.getSortOrder());
        }
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

    List<GroupLearningOutcome> findByCourseId(@Param("courseId") UUID courseId);

    Optional<GroupLearningOutcome> findById(@Param("id") UUID id);

    int findMaxSortOrderByCourseId(@Param("courseId") UUID courseId);

    void updateTreePosition(@Param("id") UUID id, @Param("courseId") UUID courseId, @Param("code") String code, @Param("sortOrder") int sortOrder);

    void deleteById(@Param("id") UUID id, @Param("courseId") UUID courseId);

    void deleteByCourseId(@Param("courseId") UUID courseId);
}
