package edu.ai.lms.core.attempt.api;

import java.time.LocalDateTime;
import java.util.UUID;

/** Published after an attempt has transitioned durably to FINISHED. */
public record AttemptFinishedEvent(
        UUID attemptId,
        UUID studentId,
        UUID courseId,
        LocalDateTime submittedAt
) {
}
