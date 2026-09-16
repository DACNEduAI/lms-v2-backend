package edu.ai.lms.core.learnermodel.app.domain;

import java.util.List;
import java.util.UUID;

public record LearningOutcomePrediction(
        UUID learningOutcomeId,
        Double probability,
        PredictionStatus status,
        List<UUID> basisQuestionIds
) {
    public LearningOutcomePrediction {
        basisQuestionIds = basisQuestionIds == null ? List.of() : List.copyOf(basisQuestionIds);
    }
}
