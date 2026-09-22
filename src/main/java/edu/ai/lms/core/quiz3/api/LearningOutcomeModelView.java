package edu.ai.lms.core.quiz3.api;

import java.util.UUID;

public record LearningOutcomeModelView(
        UUID learningOutcomeId,
        long ktLearningOutcomeId
) {
}
