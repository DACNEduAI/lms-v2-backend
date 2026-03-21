package edu.ai.lms.core.attempt.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attempt {

    public enum AttemptStatus {
        STARTED,
        FINISHED,
        ERROR
    }

    private UUID id;

    private UUID studentId;

    private UUID quizId;

    private Double score;

    private LocalDateTime startTime;

    private LocalDateTime dueTime;

    private LocalDateTime submittedTime;

    private Integer timeSpentSeconds;

    private AttemptStatus status;

    private String errorReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
