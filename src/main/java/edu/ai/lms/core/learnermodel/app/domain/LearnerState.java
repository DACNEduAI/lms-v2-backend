package edu.ai.lms.core.learnermodel.app.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LearnerState(
        UUID id,
        UUID studentId,
        UUID courseId,
        UUID attemptId,
        LocalDateTime calculatedAt,
        List<LearnerStateOutcome> outcomes
) {
    public LearnerState {
        outcomes = outcomes == null ? List.of() : List.copyOf(outcomes);
    }
}
