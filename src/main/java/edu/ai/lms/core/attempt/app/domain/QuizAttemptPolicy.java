package edu.ai.lms.core.attempt.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Snapshot of quiz attempt constraints used by attempt-start and submit flows.
 *
 * <p>The policy combines schedule and timer controls resolved from quiz configuration:
 * <ul>
 *     <li>{@code unlimited}: whether the quiz is not constrained by start/due window.</li>
 *     <li>{@code timeLimitMinutes}: per-attempt timer length when timer mode is enabled.</li>
 *     <li>{@code quizStartTime}/{@code quizDueTime}: quiz availability and deadline window.</li>
 *     <li>{@code attemptLimited}: maximum number of finished attempts per student.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttemptPolicy {

    private UUID quizId;

    private UUID quizReleaseId;

    private UUID courseId;

    private Boolean unlimited;

    private Integer timeLimitMinutes;

    private LocalDateTime quizStartTime;

    private LocalDateTime quizDueTime;

    private Integer attemptLimited;

    /**
     * Returns whether per-attempt timer mode is active.
     *
     * <p>Timer mode is enabled only when:
     * <ul>
     *     <li>quiz is not unlimited, and</li>
     *     <li>timeLimitMinutes is provided and greater than 0.</li>
     * </ul>
     */
    public boolean hasTimer() {
        return !Boolean.TRUE.equals(unlimited) && timeLimitMinutes != null && timeLimitMinutes > 0;
    }

    /**
     * Returns true when quiz scheduling is unlimited.
     */
    public boolean isUnlimited() {
        return Boolean.TRUE.equals(unlimited);
    }
}
