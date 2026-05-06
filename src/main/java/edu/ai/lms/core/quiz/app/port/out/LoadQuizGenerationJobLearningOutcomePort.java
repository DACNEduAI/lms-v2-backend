package edu.ai.lms.core.quiz.app.port.out;

import java.util.List;
import java.util.UUID;

public interface LoadQuizGenerationJobLearningOutcomePort {
    List<UUID> findLearningOutcomeIdsByJobId(UUID jobId);
}
