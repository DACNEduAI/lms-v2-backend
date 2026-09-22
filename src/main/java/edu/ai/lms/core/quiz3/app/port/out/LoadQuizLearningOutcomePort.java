package edu.ai.lms.core.quiz3.app.port.out;

import java.util.List;
import java.util.UUID;

public interface LoadQuizLearningOutcomePort {
    List<UUID> findLearningOutcomeIdsByQuizId(UUID quizId);
}
