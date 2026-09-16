package edu.ai.lms.core.learnermodel.app.domain;

import java.util.List;

public record ModelOutcomePrediction(
        long ktLearningOutcomeId,
        Double probability,
        PredictionStatus status,
        List<Long> basisKtQuestionIds
) {
    public ModelOutcomePrediction {
        basisKtQuestionIds = basisKtQuestionIds == null ? List.of() : List.copyOf(basisKtQuestionIds);
    }
}
