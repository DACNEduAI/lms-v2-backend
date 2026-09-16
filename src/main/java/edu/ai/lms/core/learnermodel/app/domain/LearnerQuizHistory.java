package edu.ai.lms.core.learnermodel.app.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LearnerQuizHistory(
        UUID quizId,
        UUID attemptId,
        OffsetDateTime submittedAt,
        List<LearnerInteraction> interactions
) {
    public LearnerQuizHistory {
        interactions = interactions == null ? List.of() : List.copyOf(interactions);
    }
}
