package edu.ai.lms.core.attempt.api;

import java.util.UUID;

public record LearnerInteractionView(
        UUID questionId,
        long ktQuestionId,
        boolean correct,
        UUID learningOutcomeId,
        Long ktLearningOutcomeId,
        int learningOutcomeMappingCount
) {
}
