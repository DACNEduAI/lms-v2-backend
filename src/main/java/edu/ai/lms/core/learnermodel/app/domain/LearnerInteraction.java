package edu.ai.lms.core.learnermodel.app.domain;

import java.util.UUID;

public record LearnerInteraction(
        UUID questionId,
        long ktQuestionId,
        boolean correct,
        UUID learningOutcomeId,
        Long ktLearningOutcomeId,
        int learningOutcomeMappingCount
) {
}
