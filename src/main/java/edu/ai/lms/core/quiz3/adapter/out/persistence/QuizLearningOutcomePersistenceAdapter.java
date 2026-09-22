package edu.ai.lms.core.quiz3.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz3.app.port.out.LoadQuizLearningOutcomePort;
import edu.ai.lms.core.quiz3.app.port.out.SaveQuizLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuizLearningOutcomePersistenceAdapter implements SaveQuizLearningOutcomePort,
        LoadQuizLearningOutcomePort {

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

}
