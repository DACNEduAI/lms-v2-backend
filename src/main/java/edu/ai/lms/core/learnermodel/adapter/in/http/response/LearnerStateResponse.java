package edu.ai.lms.core.learnermodel.adapter.in.http.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LearnerStateResponse(
        UUID attemptId,
        LocalDateTime calculatedAt,
        List<LearnerStateOutcomeResponse> outcomes
) {
    public LearnerStateResponse {
        outcomes = outcomes == null ? List.of() : List.copyOf(outcomes);
    }
}
