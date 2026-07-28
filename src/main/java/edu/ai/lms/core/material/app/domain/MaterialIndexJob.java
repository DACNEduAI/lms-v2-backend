package edu.ai.lms.core.material.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialIndexJob {
    public enum Operation { INGEST, DELETE }
    public enum Status { PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED }

    private UUID id;
    private UUID courseId;
    private UUID materialId;
    private Operation operation;
    private UUID revision;
    private String fileKey;
    private Status status;
    private int attemptCount;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime leaseUntil;
    private String errorMessage;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
