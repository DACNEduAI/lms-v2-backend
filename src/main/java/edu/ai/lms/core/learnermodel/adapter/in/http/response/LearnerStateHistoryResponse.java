package edu.ai.lms.core.learnermodel.adapter.in.http.response;

import java.util.List;
import java.util.UUID;

public record LearnerStateHistoryResponse(
        UUID courseId,
        UUID studentId,
        List<LearnerStateResponse> states
) {
    public LearnerStateHistoryResponse {
        states = states == null ? List.of() : List.copyOf(states);
    }
}
