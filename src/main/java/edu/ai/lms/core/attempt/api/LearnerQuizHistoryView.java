package edu.ai.lms.core.attempt.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LearnerQuizHistoryView(
        UUID quizId,
        UUID attemptId,
        LocalDateTime submittedAt,
        List<LearnerInteractionView> interactions
) {
    public LearnerQuizHistoryView {
        interactions = interactions == null ? List.of() : List.copyOf(interactions);
    }
}
