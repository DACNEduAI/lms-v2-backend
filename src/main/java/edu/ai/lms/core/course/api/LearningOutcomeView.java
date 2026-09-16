package edu.ai.lms.core.course.api;

import java.util.UUID;

public record LearningOutcomeView(
        UUID id,
        long ktLearningOutcomeId,
        String code,
        Integer prefixNumber,
        String description,
        UUID groupId,
        String groupCode,
        String groupDescription
) {
}
