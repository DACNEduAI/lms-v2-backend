package edu.ai.lms.core.quiz.api;

import java.time.LocalDateTime;
import java.util.UUID;

public record AttemptPolicyView(
        UUID quizId,
        UUID courseId,
        boolean unlimited,
        Integer timeLimitMinutes,
        LocalDateTime startTime,
        LocalDateTime dueTime,
        Integer attemptLimit
) {
}
