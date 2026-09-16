package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.port.out.AttemptCodeRunnerPort;
import edu.ai.lms.core.attempt.app.port.out.AttemptCodeSubmissionSnapshotPort;
import edu.ai.lms.core.attempt.app.port.out.AttemptQuestionResultPort;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.attempt.app.port.out.SaveAttemptPort;
import edu.ai.lms.core.coderunner.api.CodeSubmissionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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
    private final AttemptCodeSubmissionSnapshotPort codeSubmissionSnapshotPort;
    private final AttemptQuestionResultPort attemptQuestionResultPort;

    @EventListener
    @Transactional
    public void onCodeSubmissionCompleted(CodeSubmissionCompletedEvent event) {
        if (event.attemptId() == null) return;
        
        Attempt attempt = loadAttemptPort.getById(event.attemptId()).orElse(null);
        if (attempt == null || attempt.getStatus() != Attempt.AttemptStatus.FINALIZING) {
            return;
        }

        Map<UUID, UUID> selected = codeSubmissionSnapshotPort.findByAttemptId(attempt.getId());
        if (!selected.entrySet().stream().allMatch(item -> codeRunnerPort.isTerminalSubmit(
                item.getValue(), attempt.getId(), attempt.getStudentId(), item.getKey()))) {
            log.info("Attempt {} is FINALIZING but selected Code Runner submissions are still pending", attempt.getId());
            return;
        }

        finalizeAttempt(attempt, selected);
    }

    /**
     * Completion events are intentionally only a fast path. If the process
     * stops after a Code Runner submission is committed but before its event is
     * handled, this scan completes the persisted FINALIZING attempt.
     */
    @Scheduled(fixedDelayString = "${attempt.finalization-recovery.delay-ms:30000}")
    @Transactional
    public void recoverFinalizingAttempts() {
        for (Attempt attempt : loadAttemptPort.getFinalizingAttempts()) {
            Map<UUID, UUID> selected = codeSubmissionSnapshotPort.findByAttemptId(attempt.getId());
            if (selected.entrySet().stream().allMatch(item -> codeRunnerPort.isTerminalSubmit(
                    item.getValue(), attempt.getId(), attempt.getStudentId(), item.getKey()))) {
                finalizeAttempt(attempt, selected);
            }
        }
    }

    private void finalizeAttempt(Attempt attempt, Map<UUID, UUID> selectedSubmissions) {
        log.info("Finalizing attempt {}", attempt.getId());
        List<QuestionAnswerKey> answerKeys = policyPort.getQuestionAnswerKeys(attempt.getQuizId());
        
        Map<UUID, QuestionAnswerKey.QuestionType> questionTypes = new java.util.LinkedHashMap<>();
        answerKeys.forEach(key -> questionTypes.putIfAbsent(key.getQuestionId(), key.getQuestionType()));
        long totalGradable = questionTypes.values().stream()
                .filter(type -> type == QuestionAnswerKey.QuestionType.MULTIPLE_CHOICE
                        || type == QuestionAnswerKey.QuestionType.TRUE_FALSE
                        || type == QuestionAnswerKey.QuestionType.CODE_RUNNER)
                .count();

        if (totalGradable == 0) {
            saveAttemptPort.finalizeIfStillFinalizing(attempt.getId(), attempt.getScore() != null ? attempt.getScore() : 0.0);
            return;
        }

        double codeRunnerEarnedRatios = 0;
        for (Map.Entry<UUID, QuestionAnswerKey.QuestionType> question : questionTypes.entrySet()) {
            if (question.getValue() == QuestionAnswerKey.QuestionType.CODE_RUNNER) {
                UUID submissionId = selectedSubmissions.get(question.getKey());
                if (submissionId == null) continue;
                OptionalDouble ratio = codeRunnerPort.findCompletedSubmitRatio(
                        submissionId, attempt.getId(), attempt.getStudentId(), question.getKey());
                if (ratio.isPresent()) {
                    codeRunnerEarnedRatios += ratio.getAsDouble();
                    attemptQuestionResultPort.updateCorrectness(
                            attempt.getId(), question.getKey(), ratio.getAsDouble() >= 1.0d);
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
