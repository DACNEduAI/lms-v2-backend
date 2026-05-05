package edu.ai.lms.core.quiz.app.port.out;

import java.util.List;
import java.util.UUID;

public interface SaveQuizGenerationJobLearningOutcomePort {
    void addLearningOutcomes(UUID jobId, List<UUID> learningOutcomeIds);
}
