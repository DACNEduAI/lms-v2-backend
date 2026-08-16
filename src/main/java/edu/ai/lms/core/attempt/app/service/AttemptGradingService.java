package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.port.out.AttemptCodeRunnerPort;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.attempt.app.port.out.SaveAttemptPort;
import edu.ai.lms.core.coderunner.api.CodeSubmissionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttemptGradingService {

    private final LoadAttemptPort loadAttemptPort;
    private final SaveAttemptPort saveAttemptPort;
    private final LoadQuizAttemptPolicyPort policyPort;
    private final AttemptCodeRunnerPort codeRunnerPort;

    @EventListener
    @Transactional
    public void onCodeSubmissionCompleted(CodeSubmissionCompletedEvent event) {
        if (event.attemptId() == null) return;
        
        Attempt attempt = loadAttemptPort.getById(event.attemptId()).orElse(null);
        if (attempt == null || attempt.getStatus() != Attempt.AttemptStatus.FINALIZING) {
            return;
        }

        if (codeRunnerPort.hasPendingSubmissions(attempt.getId())) {
            log.info("Attempt {} is FINALIZING but still has pending code submissions", attempt.getId());
            return;
        }

        finalizeAttempt(attempt);
    }

    private void finalizeAttempt(Attempt attempt) {
        log.info("Finalizing attempt {}", attempt.getId());
        List<QuestionAnswerKey> answerKeys = policyPort.getQuestionAnswerKeys(attempt.getQuizId());
        
        long totalGradable = answerKeys.stream()
                .filter(k -> k.getQuestionType() != QuestionAnswerKey.QuestionType.ESSAY)
                .count();

        if (totalGradable == 0) {
            saveAttemptPort.finalizeIfStillFinalizing(attempt.getId(), attempt.getScore() != null ? attempt.getScore() : 0.0);
            return;
        }

        double codeRunnerEarnedRatios = 0;
        for (QuestionAnswerKey key : answerKeys) {
            if (key.getQuestionType() == QuestionAnswerKey.QuestionType.CODE_RUNNER) {
                OptionalDouble ratio = codeRunnerPort.findLatestCompletedSubmitRatio(attempt.getId(), key.getQuestionId());
                if (ratio.isPresent()) {
                    codeRunnerEarnedRatios += ratio.getAsDouble();
                }
            }
        }

        double finalScore = (attempt.getScore() != null ? attempt.getScore() : 0.0) 
                            + (codeRunnerEarnedRatios / totalGradable) * AttemptScoringService.DEFAULT_MAX_SCORE;
        finalScore = Math.round(finalScore * 100.0) / 100.0;
        
        saveAttemptPort.finalizeIfStillFinalizing(attempt.getId(), finalScore);
        log.info("Attempt {} finalized with score {}", attempt.getId(), finalScore);
    }
}
