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
        FINALIZING,
        FINISHED,
        ERROR
    }

    private UUID id;

    private UUID studentId;

    private UUID quizId;

    private UUID quizReleaseId;

    private Double score;

    private LocalDateTime startTime;

    private LocalDateTime dueTime;

    private LocalDateTime submittedTime;

    private LocalDateTime finishedTime;

    private Integer timeSpentSeconds;

    private String errorReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private AttemptStatus status;

    private Double percentage;

    public void updateScore(Double maxScore) {
        if (maxScore == null || maxScore == 0.0) {
            this.percentage = 100.0;
        } else {
            this.percentage = (this.score / maxScore) * 100;
        }
    }
}
