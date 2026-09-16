package edu.ai.lms.core.learnermodel.adapter.in.http.response;

import java.util.UUID;

public record SingleLearnerModelPredictionResponse(
        UUID courseId,
        UUID studentId,
        String model,
        int historyInteractionCount,
        LearningOutcomePredictionResponse prediction
) {
}
