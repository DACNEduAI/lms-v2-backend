package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.app.domain.QuizLearningOutcomeView;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuizLearningOutcomePersistenceAdapter implements SaveQuizLearningOutcomePort,
        LoadQuizLearningOutcomePort,
        LoadCourseLearningOutcomePort {

    private final QuizLearningOutcomeRepository quizLearningOutcomeRepository;

    @Override
    public void addLearningOutcomes(UUID quizId, List<UUID> learningOutcomeIds) {
        if (learningOutcomeIds == null || learningOutcomeIds.isEmpty()) {
            return;
        }
        quizLearningOutcomeRepository.insertBatch(quizId, learningOutcomeIds);
    }

    @Override
    public void removeLearningOutcomes(UUID quizId, List<UUID> learningOutcomeIds) {
        if (learningOutcomeIds == null || learningOutcomeIds.isEmpty()) {
            return;
        }
        quizLearningOutcomeRepository.deleteBatch(quizId, learningOutcomeIds);
    }

    @Override
    public List<UUID> findLearningOutcomeIdsByQuizId(UUID quizId) {
        return quizLearningOutcomeRepository.findLearningOutcomeIdsByQuizId(quizId);
    }

    @Override
    public List<UUID> findExistingLearningOutcomeIdsByCourseIdAndIds(UUID courseId, List<UUID> learningOutcomeIds) {
        if (learningOutcomeIds == null || learningOutcomeIds.isEmpty()) {
            return List.of();
        }
        return quizLearningOutcomeRepository.findExistingLearningOutcomeIdsByCourseIdAndIds(courseId, learningOutcomeIds);
    }

    @Override
    public List<UUID> findExistingGroupLearningOutcomeIdsByCourseIdAndIds(UUID courseId, List<UUID> groupLearningOutcomeIds) {
        if (groupLearningOutcomeIds == null || groupLearningOutcomeIds.isEmpty()) {
            return List.of();
        }
        return quizLearningOutcomeRepository.findExistingGroupLearningOutcomeIdsByCourseIdAndIds(courseId, groupLearningOutcomeIds);
    }

    @Override
    public List<UUID> findLearningOutcomeIdsByCourseIdAndGroupIds(UUID courseId, List<UUID> groupLearningOutcomeIds) {
        if (groupLearningOutcomeIds == null || groupLearningOutcomeIds.isEmpty()) {
            return List.of();
        }
        return quizLearningOutcomeRepository.findLearningOutcomeIdsByCourseIdAndGroupIds(courseId, groupLearningOutcomeIds);
    }

    @Override
    public List<QuizLearningOutcomeView> findQuizLearningOutcomes(UUID courseId, UUID quizId) {
        return quizLearningOutcomeRepository.findQuizLearningOutcomes(courseId, quizId);
    }
}
