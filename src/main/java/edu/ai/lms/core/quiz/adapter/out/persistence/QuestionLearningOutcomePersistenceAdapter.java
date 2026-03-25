package edu.ai.lms.core.quiz.adapter.out.persistence;

import edu.ai.lms.core.quiz.app.domain.QuestionLearningOutcomeView;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionLearningOutcomePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class QuestionLearningOutcomePersistenceAdapter implements SaveQuestionLearningOutcomePort, LoadQuestionLearningOutcomePort {

    private final QuestionLearningOutcomeRepository questionLearningOutcomeRepository;

    @Override
    public void addLearningOutcomes(UUID questionId, List<UUID> learningOutcomeIds) {
        if (learningOutcomeIds == null || learningOutcomeIds.isEmpty()) {
            return;
        }
        questionLearningOutcomeRepository.insertBatch(questionId, learningOutcomeIds);
    }

    @Override
    public void removeLearningOutcomes(UUID questionId, List<UUID> learningOutcomeIds) {
        if (learningOutcomeIds == null || learningOutcomeIds.isEmpty()) {
            return;
        }
        questionLearningOutcomeRepository.deleteBatch(questionId, learningOutcomeIds);
    }

    @Override
    public List<QuestionLearningOutcomeView> findLearningOutcomesByQuestionId(UUID questionId) {
        return questionLearningOutcomeRepository.findLearningOutcomesByQuestionId(questionId);
    }

    @Override
    public List<UUID> findLearningOutcomeIdsByQuestionId(UUID questionId) {
        return questionLearningOutcomeRepository.findLearningOutcomeIdsByQuestionId(questionId);
    }
}
