package edu.ai.lms.core.quiz.api;

import java.util.UUID;

public record LearningOutcomeModelView(
        UUID learningOutcomeId,
        long ktLearningOutcomeId
) {
}
