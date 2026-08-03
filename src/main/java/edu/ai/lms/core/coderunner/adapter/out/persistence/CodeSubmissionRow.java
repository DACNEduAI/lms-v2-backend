package edu.ai.lms.core.coderunner.adapter.out.persistence;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
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
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
