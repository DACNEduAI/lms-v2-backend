package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.QuizAttemptPolicy;
import edu.ai.lms.core.attempt.app.domain.AttemptQuestionResult;
import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.port.out.AttemptQuestionResultPort;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.attempt.app.port.out.SaveAttemptPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
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
 * </ul>
 * Module context:
 * <ul>
 *     <li>Belongs to the attempt application service layer: {@code core.attempt.app.service}</li>
 *     <li>Used by attempt use-case services and Quartz auto-close jobs</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttemptAutoCloseService {

    /**
     * Grace window added after due time to tolerate small client/network delays during submission.
     */
    private static final long SUBMISSION_GRACE_SECONDS = 1L;

    private final LoadAttemptPort loadAttemptPort;
    private final LoadQuizAttemptPolicyPort loadQuizAttemptPolicyPort;
    private final SaveAttemptPort saveAttemptPort;
    private final AttemptQuestionResultPort attemptQuestionResultPort;

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
     * @param courseId  current course id used to load quiz attempt policy
     * @param quizId    current quiz id used to locate started attempt and policy
     * @param now       current timestamp supplied by caller for deterministic evaluation
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
     * Quartz job entrypoint. Closes one attempt only when it is still started and expired.
     *
     * <p>This method is intentionally idempotent and safe for repeated executions.
     */
    @Transactional
    public void closeAttemptIfStillStarted(UUID attemptId, LocalDateTime now) {
        Optional<Attempt> attemptOpt = loadAttemptPort.getById(attemptId);
        if (attemptOpt.isEmpty()) {
            return;
        }

        Attempt attempt = attemptOpt.get();
        if (attempt.getStatus() != Attempt.AttemptStatus.STARTED) {
            return;
        }

        if (!isExpired(attempt.getDueTime(), now)) {
            return;
        }

        loadQuizAttemptPolicyPort
                .getQuizAttemptPolicy(null, attempt.getQuizId())
                .ifPresent(policy -> closeExpiredStartedAttempt(attempt, policy, now));

        log.info("Auto-closed expired attempt with id={} in the closeAttemptIfStillStarted function", attemptId);
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
     * Internal usage: called by
     * {@link #isAlreadyStartedAttemptExpiredValidation(UUID, UUID, UUID, LocalDateTime)} and
     * {@link #closeAttemptIfStillStarted(UUID, LocalDateTime)}.
     *
     * @param attempt expired started attempt to mutate and save
     * @param policy  quiz attempt policy that determines timer-based close behavior
     * @param now     timestamp recorded as the submitted time
     */
    private boolean closeExpiredStartedAttempt(Attempt attempt, QuizAttemptPolicy policy, LocalDateTime now) {
        Attempt.AttemptStatus status;
        Double score;
        String errorReason;

        if (policy.hasTimer()) {
            status = Attempt.AttemptStatus.FINISHED;
            errorReason = "Attempt expired before submission";
            score = 0.0;
        } else {
            status = Attempt.AttemptStatus.FINISHED;
            score = 0.0;
            errorReason = "Attempt was not submitted before due time";
        }

        boolean closed = saveAttemptPort.closeIfStarted(attempt.getId(), status, score, errorReason, now);
        if (closed) {
            saveAllWrongRecords(attempt, loadQuizAttemptPolicyPort.getQuestionAnswerKeys(attempt.getQuizId()));
        }
        return closed;
    }

    private void saveAllWrongRecords(Attempt attempt, List<QuestionAnswerKey> answerKeys) {
        List<AttemptQuestionResult> records = new java.util.ArrayList<>();
        int order = 0;
        for (QuestionAnswerKey key : answerKeys) {
            if (key.getQuestionType() == QuestionAnswerKey.QuestionType.ESSAY) {
                order++;
                continue;
            }
            QuestionAnswerKey.AnswerSnapshot correctAnswer = key.getQuestionType() == QuestionAnswerKey.QuestionType.CODE_RUNNER
                    ? null : key.getAnswers().stream().filter(QuestionAnswerKey.AnswerSnapshot::correct).findFirst().orElse(null);
            int mappingCount = key.getLearningOutcomes() == null ? 0 : key.getLearningOutcomes().size();
            QuestionAnswerKey.LearningOutcomeSnapshot outcome = mappingCount == 1
                    ? key.getLearningOutcomes().getFirst() : null;
            records.add(AttemptQuestionResult.builder()
                    .attemptId(attempt.getId())
                    .questionId(key.getQuestionId())
                    .ktQuestionId(key.getKtQuestionId())
                    .interactionOrder(order)
                    .questionType(key.getQuestionType().name())
                    .questionContent(key.getQuestionContent())
                    .questionContentFormat(key.getQuestionContentFormat())
                    .correctAnswerNumber(correctAnswer == null ? null : correctAnswer.answerNumber())
                    .correctAnswerPosition(correctAnswer == null ? null : key.getAnswers().indexOf(correctAnswer))
                    .correctAnswerContent(correctAnswer == null ? null : correctAnswer.content())
                    .correctAnswerContentFormat(correctAnswer == null ? null : correctAnswer.contentFormat())
                    .correct(false)
                    .learningOutcomeId(outcome == null ? null : outcome.learningOutcomeId())
                    .ktLearningOutcomeId(outcome == null ? null : outcome.ktLearningOutcomeId())
                    .learningOutcomeMappingCount(mappingCount)
                    .snapshotComplete(true)
                    .build());
            order++;
        }
        attemptQuestionResultPort.upsertAll(records);
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
     * @param now     current time used for comparison
     * @return {@code true} if {@code now} is strictly after {@code dueTime + SUBMISSION_GRACE_SECONDS}; otherwise {@code false}
     */
    private boolean isExpired(LocalDateTime dueTime, LocalDateTime now) {
        return dueTime != null && now.isAfter(dueTime.plusSeconds(SUBMISSION_GRACE_SECONDS));
    }
}
