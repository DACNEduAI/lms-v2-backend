package edu.ai.lms.core.learnermodel.adapter.in.http.response;

import java.util.List;
import java.util.UUID;

public record AllLearnerModelPredictionsResponse(
        UUID courseId,
        UUID studentId,
        String model,
        int historyInteractionCount,
        List<LearningOutcomePredictionResponse> predictions
) {
    public AllLearnerModelPredictionsResponse {
        predictions = predictions == null ? List.of() : List.copyOf(predictions);
    }
}
