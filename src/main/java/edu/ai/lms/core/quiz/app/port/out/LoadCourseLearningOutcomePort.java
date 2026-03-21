package edu.ai.lms.core.quiz.app.port.out;

import java.util.List;
import java.util.UUID;

public interface LoadCourseLearningOutcomePort {

    List<UUID> findExistingLearningOutcomeIdsByCourseIdAndIds(UUID courseId, List<UUID> learningOutcomeIds);

    List<UUID> findExistingGroupLearningOutcomeIdsByCourseIdAndIds(UUID courseId, List<UUID> groupLearningOutcomeIds);

    List<UUID> findLearningOutcomeIdsByCourseIdAndGroupIds(UUID courseId, List<UUID> groupLearningOutcomeIds);
}
