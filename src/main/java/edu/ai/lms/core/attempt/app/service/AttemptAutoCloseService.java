package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.core.attempt.adapter.in.scheduler.AttemptAutoCloseScheduler;
import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.QuizAttemptPolicy;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.attempt.app.port.out.SaveAttemptPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Auto-closes quiz attempts that remain in {@link Attempt.AttemptStatus#STARTED}
 * after their due time (with a small submission grace period).
 * <p>
 * This service centralizes expiration handling so different flows always apply the same
 * business rule before continuing. When an attempt expires:
 * <ul>
 *     <li>If the quiz policy has a timer, the attempt is closed as {@code ERROR} with no score.</li>
 *     <li>If the quiz policy has no timer, the attempt is closed as {@code FINISHED} with score {@code 0.0}.</li>
 * </ul>
 * In both cases, {@code submittedTime} is set to the processing time and the updated
 * attempt is persisted via {@link SaveAttemptPort}.
 * <p>
 * Main callers in this codebase:
 * <ul>
 *     <li>{@link StartAttemptService#startAttempt(edu.ai.lms.core.attempt.app.port.in.command.StartAttemptCommand)}</li>
 *     <li>{@link GetMyAttemptsService#getMyAttempts(UUID, UUID)}</li>
 *     <li>{@link SubmitAttemptService#submitAttempt(edu.ai.lms.core.attempt.app.port.in.command.SubmitAttemptCommand)}</li>
 *     <li>{@link AttemptAutoCloseScheduler#closeExpiredStartedAttemptsScheduler()}</li>
 * </ul>
 * Module context:
 * <ul>
 *     <li>Belongs to the attempt application service layer: {@code core.attempt.app.service}</li>
 *     <li>Used by attempt use-case services and the scheduler input adapter</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AttemptAutoCloseService {

    /**
     * Grace window added after due time to tolerate small client/network delays during submission.
     */
    private static final long SUBMISSION_GRACE_SECONDS = 5L;

    private final LoadAttemptPort loadAttemptPort;
    private final LoadQuizAttemptPolicyPort loadQuizAttemptPolicyPort;
    private final SaveAttemptPort saveAttemptPort;

    /**
     * Closes the latest active started attempt for a student and quiz if that attempt is expired.
     * <p>
     * Business flow:
     * <ol>
     *     <li>Loads the latest {@code STARTED} attempt of the student for the quiz.</li>
     *     <li>If no started attempt exists, exits without side effects.</li>
     *     <li>Checks expiration against {@code dueTime + SUBMISSION_GRACE_SECONDS}.</li>
     *     <li>If expired, loads quiz policy and applies close rules through
     *     {@link #closeExpiredStartedAttempt(Attempt, QuizAttemptPolicy, LocalDateTime)}.</li>
     * </ol>
     * Output/side effect: may persist one closed attempt; returns no value.
     * <p>
     * Used by:
     * <ul>
     *     <li>{@link StartAttemptService#startAttempt(edu.ai.lms.core.attempt.app.port.in.command.StartAttemptCommand)}</li>
     *     <li>{@link GetMyAttemptsService#getMyAttempts(UUID, UUID)}</li>
     *     <li>{@link SubmitAttemptService#submitAttempt(edu.ai.lms.core.attempt.app.port.in.command.SubmitAttemptCommand)}</li>
     * </ul>
     *
     * @param studentId current student id whose started attempt is validated
     * @param courseId current course id used to load quiz attempt policy
     * @param quizId current quiz id used to locate started attempt and policy
     * @param now current timestamp supplied by caller for deterministic evaluation
     */
    @Transactional
    public void isAlreadyStartedAttemptExpiredValidation(UUID studentId, UUID courseId, UUID quizId, LocalDateTime now) {
        Optional<Attempt> startedAttemptOpt = loadAttemptPort.getLatestStartedAttempt(studentId, quizId);
        if (startedAttemptOpt.isEmpty()) {
            return;
        }

        Attempt startedAttempt = startedAttemptOpt.get();
        if (isExpired(startedAttempt.getDueTime(), now)) {
            loadQuizAttemptPolicyPort
                    .getQuizAttemptPolicy(courseId, quizId)
                    .ifPresent(policy -> closeExpiredStartedAttempt(startedAttempt, policy, now));
        }
    }

    /**
     * Closes all expired started attempts in batch at execution time.
     * <p>
     * Business flow:
     * <ol>
     *     <li>Computes threshold as {@code now - SUBMISSION_GRACE_SECONDS}.</li>
     *     <li>Loads all attempts still in {@code STARTED} state whose due time is before threshold.</li>
     *     <li>For each attempt, loads its quiz policy.</li>
     *     <li>Skips attempts without policy and closes valid ones with
     *     {@link #closeExpiredStartedAttempt(Attempt, QuizAttemptPolicy, LocalDateTime)}.</li>
     * </ol>
     * Output/side effect: may persist multiple closed attempts; returns no value.
     * <p>
     * Used by:
     * <ul>
     *     <li>{@link AttemptAutoCloseScheduler#closeExpiredStartedAttemptsScheduler()}</li>
     * </ul>
     */
    @Transactional
    public void closeAllExpiredStartedAttempts() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusSeconds(SUBMISSION_GRACE_SECONDS);
        List<Attempt> expiredAttempts = loadAttemptPort.getExpiredStartedAttempts(threshold);

        for (Attempt attempt : expiredAttempts) {

            QuizAttemptPolicy policy = loadQuizAttemptPolicyPort
                    .getQuizAttemptPolicy(null, attempt.getQuizId())
                    // null because 
                    .orElse(null);
            if (policy == null) {
                continue;
            }
            closeExpiredStartedAttempt(attempt, policy, now);
        }
    }

    /**
     * Applies the close rule for an expired attempt based on quiz policy and persists it.
     * <p>
     * Rule details:
     * <ul>
     *     <li>Timer enabled: status {@code ERROR}, score {@code null}, reason indicates expiration.</li>
     *     <li>Timer disabled: status {@code FINISHED}, score {@code 0.0}, reason indicates non-submission.</li>
     * </ul>
     * Always sets {@code submittedTime} to {@code now} before persisting.
     * <p>
     * Internal usage:
     * <ul>
     *     <li>Called by {@link #isAlreadyStartedAttemptExpiredValidation(UUID, UUID, UUID, LocalDateTime)}</li>
     *     <li>Called by {@link #closeAllExpiredStartedAttempts()}</li>
     * </ul>
     *
     * @param attempt expired started attempt to mutate and save
     * @param policy quiz attempt policy that determines timer-based close behavior
     * @param now timestamp recorded as the submitted time
     */
    private void closeExpiredStartedAttempt(Attempt attempt, QuizAttemptPolicy policy, LocalDateTime now) {
        if (policy.hasTimer()) {
            attempt.setStatus(Attempt.AttemptStatus.ERROR);
            attempt.setErrorReason("Attempt expired before submission");
            attempt.setScore(null);
        } else {
            attempt.setStatus(Attempt.AttemptStatus.FINISHED);
            attempt.setScore(0.0);
            attempt.setErrorReason("Attempt was not submitted before due time");
        }

        attempt.setSubmittedTime(now);
        saveAttemptPort.save(attempt);
    }

    /**
     * Checks whether due time has passed including the grace window.
     * <p>
     * Internal usage:
     * <ul>
     *     <li>Called by {@link #isAlreadyStartedAttemptExpiredValidation(UUID, UUID, UUID, LocalDateTime)}</li>
     * </ul>
     *
     * @param dueTime due time of the attempt; {@code null} means not expired by this check
     * @param now current time used for comparison
     * @return {@code true} if {@code now} is strictly after {@code dueTime + SUBMISSION_GRACE_SECONDS}; otherwise {@code false}
     */
    private boolean isExpired(LocalDateTime dueTime, LocalDateTime now) {
        return dueTime != null && now.isAfter(dueTime.plusSeconds(SUBMISSION_GRACE_SECONDS));
    }
}
