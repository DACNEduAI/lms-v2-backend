package edu.ai.lms.core.attempt.app.service;

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

@Service
@RequiredArgsConstructor
public class AttemptAutoFinalizeService {

    private static final long SUBMISSION_GRACE_SECONDS = 5L;

    private final LoadAttemptPort loadAttemptPort;
    private final LoadQuizAttemptPolicyPort loadQuizAttemptPolicyPort;
    private final SaveAttemptPort saveAttemptPort;

    @Transactional
    public void finalizeExpiredStartedAttemptIfNeeded(UUID studentId, UUID quizId, LocalDateTime now) {
        Optional<Attempt> startedAttemptOpt = loadAttemptPort.getLatestStartedAttempt(studentId, quizId);
        if (startedAttemptOpt.isEmpty()) {
            return;
        }

        Attempt startedAttempt = startedAttemptOpt.get();
        if (isExpired(startedAttempt.getDueTime(), now)) {
            QuizAttemptPolicy policy = loadQuizAttemptPolicyPort
                    .getQuizAttemptPolicy(null, quizId)
                    .orElse(null);
            if (policy != null) {
                finalizeExpiredAttempt(startedAttempt, policy, now);
            }
        }
    }

    @Transactional
    public void finalizeExpiredAttempts(LocalDateTime now) {
        LocalDateTime threshold = now.minusSeconds(SUBMISSION_GRACE_SECONDS);
        List<Attempt> expiredAttempts = loadAttemptPort.getExpiredStartedAttempts(threshold);

        for (Attempt attempt : expiredAttempts) {
            QuizAttemptPolicy policy = loadQuizAttemptPolicyPort
                    .getQuizAttemptPolicy(null, attempt.getQuizId())
                    .orElse(null);
            if (policy == null) {
                continue;
            }
            finalizeExpiredAttempt(attempt, policy, now);
        }
    }

    private void finalizeExpiredAttempt(Attempt attempt, QuizAttemptPolicy policy, LocalDateTime now) {
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

    private boolean isExpired(LocalDateTime dueTime, LocalDateTime now) {
        return dueTime != null && now.isAfter(dueTime.plusSeconds(SUBMISSION_GRACE_SECONDS));
    }
}
