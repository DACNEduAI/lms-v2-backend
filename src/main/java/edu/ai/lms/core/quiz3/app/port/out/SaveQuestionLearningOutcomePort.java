package edu.ai.lms.core.quiz3.app.port.out;

import java.util.List;
import java.util.UUID;

public interface SaveQuestionLearningOutcomePort {

    void addLearningOutcomes(UUID questionId, List<UUID> learningOutcomeIds);

    void removeLearningOutcomes(UUID questionId, List<UUID> learningOutcomeIds);
}
