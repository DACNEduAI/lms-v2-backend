package edu.ai.lms.core.learnermodel.app.domain;

import java.util.List;
import java.util.UUID;

public record LearnerModelPredictionResult(
        UUID courseId,
        UUID studentId,
        LearnerModelType model,
        int historyInteractionCount,
        List<LearningOutcomePrediction> predictions
) {
    public LearnerModelPredictionResult {
        predictions = predictions == null ? List.of() : List.copyOf(predictions);
    }
}
