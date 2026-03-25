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
public class QuizAttemptPolicy {

    private UUID quizId;

    private UUID courseId;

    private Boolean unlimited;

    private Integer timeLimitMinutes;

    private LocalDateTime quizStartTime;

    private LocalDateTime quizDueTime;

    private Integer attemptLimited;

    public boolean hasTimer() {
        return !Boolean.TRUE.equals(unlimited) && timeLimitMinutes != null && timeLimitMinutes > 0;
    }

    public boolean isUnlimited() {
        return Boolean.TRUE.equals(unlimited);
    }
}
