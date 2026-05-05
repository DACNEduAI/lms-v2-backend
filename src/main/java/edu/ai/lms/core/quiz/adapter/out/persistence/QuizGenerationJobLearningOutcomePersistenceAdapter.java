package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuizGenerationJobLearningOutcomePersistenceAdapter implements SaveQuizGenerationJobLearningOutcomePort,
        LoadQuizGenerationJobLearningOutcomePort {

    private final QuizGenerationJobLearningOutcomeRepository quizGenerationJobLearningOutcomeRepository;

    @Override
    public void addLearningOutcomes(UUID jobId, List<UUID> learningOutcomeIds) {
        if (learningOutcomeIds == null || learningOutcomeIds.isEmpty()) {
            return;
        }
        quizGenerationJobLearningOutcomeRepository.insertBatch(jobId, learningOutcomeIds);
    }

    @Override
    public List<UUID> findLearningOutcomeIdsByJobId(UUID jobId) {
        return quizGenerationJobLearningOutcomeRepository.findLearningOutcomeIdsByJobId(jobId);
    }
}
