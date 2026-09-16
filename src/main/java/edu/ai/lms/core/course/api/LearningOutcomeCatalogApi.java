package edu.ai.lms.core.course.api;

import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.UUID;

public interface LearningOutcomeCatalogApi {

    Set<UUID> findExistingIds(UUID courseId, Collection<UUID> learningOutcomeIds);

    Set<UUID> findExistingGroupIds(UUID courseId, Collection<UUID> groupIds);

    List<LearningOutcomeView> findByIds(UUID courseId, Collection<UUID> learningOutcomeIds);

    List<LearningOutcomeView> findByCourseId(UUID courseId);

    List<LearningOutcomeView> findByGroupIds(UUID courseId, Collection<UUID> groupIds);
}
