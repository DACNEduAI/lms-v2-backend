package edu.ai.lms.core.learnermodel.adapter.in.http.response;

import edu.ai.lms.core.learnermodel.app.domain.PredictionStatus;

import java.util.List;
import java.util.UUID;

public record LearningOutcomePredictionResponse(
        UUID learningOutcomeId,
        Double probability,
        PredictionStatus status,
        List<UUID> basisQuestionIds
) {
    public LearningOutcomePredictionResponse {
        basisQuestionIds = basisQuestionIds == null ? List.of() : List.copyOf(basisQuestionIds);
    }
}
