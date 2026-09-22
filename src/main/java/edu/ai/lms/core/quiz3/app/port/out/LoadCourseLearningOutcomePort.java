package edu.ai.lms.core.quiz3.app.port.out;

import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.quiz3.app.domain.QuizLearningOutcomeView;

public interface LoadCourseLearningOutcomePort {

    List<UUID> findExistingLearningOutcomeIdsByCourseIdAndIds(UUID courseId, List<UUID> learningOutcomeIds);

    List<UUID> findExistingGroupLearningOutcomeIdsByCourseIdAndIds(UUID courseId, List<UUID> groupLearningOutcomeIds);

    List<UUID> findLearningOutcomeIdsByCourseIdAndGroupIds(UUID courseId, List<UUID> groupLearningOutcomeIds);

    List<QuizLearningOutcomeView> findQuizLearningOutcomes(UUID courseId, UUID quizId);
}
