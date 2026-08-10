package edu.ai.lms.core.coderunner.app.domain;

import java.time.LocalDateTime;
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
        LocalDateTime createdAt,
        LocalDateTime completedAt) {

    public enum Mode { RUN, SUBMIT }
    public enum Status { PENDING, PROCESSING, COMPLETED, FAILED }
}
