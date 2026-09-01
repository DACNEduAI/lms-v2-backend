package edu.ai.lms.core.coderunner.app.domain;

import java.util.UUID;

public record CodeSubmission(
        UUID id,
        UUID questionId,
        UUID configId,
        UUID attemptId,
        UUID studentId,
        String sourceCode,
        String idempotencyKey,
        Mode mode,
        Status status,
        String verdict,
        double score,
        double maxScore,
        java.time.Instant acceptedAt,
        java.time.Instant createdAt,
        java.time.Instant completedAt,
        int attemptCount,
        java.time.Instant nextAttemptAt) {

    public enum Mode { RUN, SUBMIT }
    public enum Status { PENDING, CLAIMED, PROCESSING, COMPLETED, FAILED, FAILED_SYSTEM }
}
