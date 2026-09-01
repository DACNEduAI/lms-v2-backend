package edu.ai.lms.core.coderunner.adapter.out.persistence;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor
class CodeSubmissionRow {
    private UUID id;
    private UUID questionId;
    private UUID configId;
    private UUID attemptId;
    private UUID studentId;
    private String sourceCode;
    private String idempotencyKey;
    private String mode;
    private String status;
    private String verdict;
    private double score;
    private double maxScore;
    private UUID workerId;
    private java.time.Instant leaseUntil;
    private java.time.Instant acceptedAt;
    private java.time.Instant createdAt;
    private java.time.Instant completedAt;
    private int attemptCount;
    private java.time.Instant nextAttemptAt;
}
