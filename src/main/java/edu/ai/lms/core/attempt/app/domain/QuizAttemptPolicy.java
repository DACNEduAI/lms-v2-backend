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

    private Integer timeLimitMinutes;

    private LocalDateTime quizDueTime;

    private Integer attemptLimited;

    public boolean hasTimer() {
        return timeLimitMinutes != null;
    }
}
