package edu.ai.lms.core.quiz.adapter.out.integration;

import edu.ai.lms.core.course.api.LearningOutcomeCatalogApi;
import edu.ai.lms.core.course.api.LearningOutcomeView;
import edu.ai.lms.core.quiz.app.domain.QuizLearningOutcomeView;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizLearningOutcomePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class CourseLearningOutcomeAdapter implements LoadCourseLearningOutcomePort {

    private final LearningOutcomeCatalogApi catalogApi;
    private final LoadQuizLearningOutcomePort associations;

    @Override
    public List<UUID> findExistingLearningOutcomeIdsByCourseIdAndIds(
            UUID courseId, List<UUID> learningOutcomeIds) {
        return List.copyOf(catalogApi.findExistingIds(courseId, learningOutcomeIds));
    }

    @Override
    public List<UUID> findExistingGroupLearningOutcomeIdsByCourseIdAndIds(
            UUID courseId, List<UUID> groupLearningOutcomeIds) {
        return List.copyOf(catalogApi.findExistingGroupIds(courseId, groupLearningOutcomeIds));
    }

    @Override
    public List<UUID> findLearningOutcomeIdsByCourseIdAndGroupIds(
            UUID courseId, List<UUID> groupLearningOutcomeIds) {
        return catalogApi.findByGroupIds(courseId, groupLearningOutcomeIds).stream()
                .map(LearningOutcomeView::id)
                .toList();
    }

    @Override
    public List<QuizLearningOutcomeView> findQuizLearningOutcomes(UUID courseId, UUID quizId) {
        return catalogApi.findByIds(courseId, associations.findLearningOutcomeIdsByQuizId(quizId)).stream()
                .map(outcome -> QuizLearningOutcomeView.builder()
                        .learningOutcomeId(outcome.id())
                        .learningOutcomeCode(outcome.code())
                        .learningOutcomePrefixNumber(outcome.prefixNumber())
                        .learningOutcomeDescription(outcome.description())
                        .groupLearningOutcomeId(outcome.groupId())
                        .groupLearningOutcomeName(outcome.groupCode())
                        .groupLearningOutcomeDescription(outcome.groupDescription())
                        .build())
                .toList();
    }
}
