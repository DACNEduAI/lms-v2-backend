package edu.ai.lms.core.course.app.domain;

import java.util.UUID;

public record CustomLearningLevelOutcomeLink(
        UUID customLearningLevelId,
        UUID learningOutcomeId,
        boolean selected) {
}
