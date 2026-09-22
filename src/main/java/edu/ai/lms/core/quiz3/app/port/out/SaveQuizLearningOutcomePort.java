package edu.ai.lms.core.quiz3.app.port.out;

import java.util.List;
import java.util.UUID;

public interface SaveQuizLearningOutcomePort {
    void addLearningOutcomes(UUID quizId, List<UUID> learningOutcomeIds);

    void removeLearningOutcomes(UUID quizId, List<UUID> learningOutcomeIds);
}
