package edu.ai.lms.core.coderunner.app.worker;

import edu.ai.lms.core.coderunner.app.domain.CodeSubmission;
import edu.ai.lms.core.coderunner.app.domain.CodeSubmissionTestResult;
import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import edu.ai.lms.core.coderunner.app.domain.StoredCodeRunnerContract;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine;
import edu.ai.lms.core.coderunner.app.port.out.CodeSubmissionPersistencePort;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerContractPort;
import edu.ai.lms.core.coderunner.app.service.CppCodeRunnerHarnessBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CodeRunnerWorker {

    private final CodeSubmissionPersistencePort submissionPort;
    private final LoadCodeRunnerContractPort contractPort;
    private final CodeExecutionEngine executionEngine;
    private final ApplicationEventPublisher eventPublisher;
    private final CppCodeRunnerHarnessBuilder harnessBuilder;
    private final int cpp17LanguageId;
    private final Duration pollingTimeout;
    private final Duration pollingInterval;

    public CodeRunnerWorker(
            CodeSubmissionPersistencePort submissionPort,
            LoadCodeRunnerContractPort contractPort,
            CodeExecutionEngine executionEngine,
            ApplicationEventPublisher eventPublisher,
            CppCodeRunnerHarnessBuilder harnessBuilder,
            @Value("${code-runner.profile.dsa-cpp17.language-id:54}") int cpp17LanguageId,
            @Value("${code-runner.verification.polling-timeout:45s}") Duration pollingTimeout,
            @Value("${code-runner.verification.polling-interval:300ms}") Duration pollingInterval) {
        this.submissionPort = submissionPort;
        this.contractPort = contractPort;
        this.executionEngine = executionEngine;
        this.eventPublisher = eventPublisher;
        this.harnessBuilder = harnessBuilder;
        this.cpp17LanguageId = cpp17LanguageId;
        this.pollingTimeout = pollingTimeout;
        this.pollingInterval = pollingInterval;
    }

    @Scheduled(fixedDelayString = "${code-runner.worker.delay-ms:1000}")
    public void processNextSubmission() {
        CodeSubmission submission = null;
        try {
            submission = acquireNext();
            if (submission == null) {
                return;
            }
            log.info("Acquired code submission for processing: {}", submission.id());
            processSubmission(submission);
        } catch (Exception e) {
            log.error("Error processing code submission", e);
            if (submission != null) {
                markFailedSystemAndNotify(submission);
            }
        }
    }

    @Transactional
    public CodeSubmission acquireNext() {
        return submissionPort.acquireNextPendingSubmission().orElse(null);
    }

    private void processSubmission(CodeSubmission submission) {
        StoredCodeRunnerContract stored = contractPort.findStoredByConfigId(submission.configId())
                .orElseThrow(() -> new IllegalStateException("Missing contract for config " + submission.configId()));

        List<ExecutableAssessmentContract.TestCase> tests = selectedTests(stored.contract(), submission.mode());
        List<CodeSubmissionTestResult> results = new ArrayList<>();
        
        try {
            for (var test : tests) {
                String harness = harnessBuilder.buildCase(stored.contract(), submission.sourceCode(), test);
                var ticket = executionEngine.submit(new CodeExecutionEngine.ExecutionRequest(
                        harness, cpp17LanguageId, null, 2_000, 5_000, 131_072, 64, 16));
                
                var engineResult = await(ticket.engineSubmissionId());
                CodeExecutionEngine.Verdict effectiveVerdict = effectiveVerdict(
                        engineResult, harnessBuilder.expectedOutput(stored.contract(), test));
                
                String verdict = effectiveVerdict.name();
                double earned = submission.mode() == CodeSubmission.Mode.SUBMIT && "ACCEPTED".equals(verdict)
                        ? test.points() : 0;
                
                CodeSubmissionTestResult result = new CodeSubmissionTestResult(
                        UUID.randomUUID(), submission.id(), requiredTestId(stored, test.id()), test.id(),
                        test.visibility(), "COMPLETED", verdict, earned, engineResult.timeMs(),
                        engineResult.memoryKb(), diagnostic(engineResult), ticket.engineSubmissionId());
                
                results.add(result);
                submissionPort.insertTestResult(result);
                
                if (effectiveVerdict == CodeExecutionEngine.Verdict.COMPILE_ERROR
                        || effectiveVerdict == CodeExecutionEngine.Verdict.INTERNAL_ERROR) break;
            }
            
            double score = results.stream().mapToDouble(CodeSubmissionTestResult::pointsEarned).sum();
            String verdict = aggregateVerdict(results, tests.size());
            submissionPort.complete(submission.id(), "COMPLETED", verdict, score);
            log.info("Finished processing submission: {}, verdict: {}, score: {}", submission.id(), verdict, score);
            eventPublisher.publishEvent(new edu.ai.lms.core.coderunner.api.CodeSubmissionCompletedEvent(submission.id(), submission.attemptId()));
        } catch (RuntimeException exception) {
            int nextAttemptCount = submission.attemptCount() + 1;
            if (nextAttemptCount >= 3) {
                log.error("Submission {} failed completely after 3 attempts: {}", submission.id(), exception.getMessage());
                markFailedSystemAndNotify(submission);
            } else {
                long backoffSeconds = (long) Math.pow(2, nextAttemptCount);
                java.time.Instant nextAttemptAt = java.time.Instant.now().plusSeconds(backoffSeconds);
                log.warn("Submission {} failed, will retry at {} (attempt {})", submission.id(), nextAttemptAt, nextAttemptCount);
                submissionPort.fail(submission.id(), "PENDING", nextAttemptCount, nextAttemptAt);
            }
        }
    }

    private void markFailedSystemAndNotify(CodeSubmission submission) {
        try {
            submissionPort.complete(submission.id(), "FAILED_SYSTEM", "INTERNAL_ERROR", 0);
            eventPublisher.publishEvent(new edu.ai.lms.core.coderunner.api.CodeSubmissionCompletedEvent(
                    submission.id(), submission.attemptId()));
        } catch (Exception exception) {
            log.error("Failed to mark submission {} as FAILED_SYSTEM", submission.id(), exception);
        }
    }

    private List<ExecutableAssessmentContract.TestCase> selectedTests(
            ExecutableAssessmentContract contract, CodeSubmission.Mode mode) {
        if (mode == CodeSubmission.Mode.RUN) {
            return contract.runtime().publicTests();
        }
        List<ExecutableAssessmentContract.TestCase> tests = new ArrayList<>();
        tests.addAll(contract.runtime().publicTests());
        tests.addAll(contract.privateGrading().hiddenTests());
        return tests;
    }

    private UUID requiredTestId(StoredCodeRunnerContract stored, String externalId) {
        UUID id = stored.testCaseIds().get(externalId);
        if (id == null) throw new IllegalStateException("Unknown test case " + externalId);
        return id;
    }

    private CodeExecutionEngine.Verdict effectiveVerdict(
            CodeExecutionEngine.ExecutionResult result, String expectedOutput) {
        if (result.verdict() == null) {
            return CodeExecutionEngine.Verdict.INTERNAL_ERROR;
        }
        if (result.verdict() == CodeExecutionEngine.Verdict.ACCEPTED
                && !expectedOutput.equals(result.stdout())) {
            return CodeExecutionEngine.Verdict.WRONG_ANSWER;
        }
        return result.verdict();
    }

    private String aggregateVerdict(List<CodeSubmissionTestResult> results, int totalTests) {
        if (results.isEmpty()) return "INTERNAL_ERROR";
        if (results.getFirst().verdict().equals("COMPILE_ERROR")) return "COMPILE_ERROR";
        
        boolean allAccepted = results.size() == totalTests
                && results.stream().allMatch(r -> "ACCEPTED".equals(r.verdict()));
        if (allAccepted) return "ACCEPTED";
        
        return results.stream()
                .filter(r -> !"ACCEPTED".equals(r.verdict()))
                .map(CodeSubmissionTestResult::verdict)
                .findFirst()
                .orElse("WRONG_ANSWER");
    }

    private CodeExecutionEngine.ExecutionResult await(String token) {
        Instant deadline = Instant.now().plus(pollingTimeout);
        while (Instant.now().isBefore(deadline)) {
            CodeExecutionEngine.ExecutionResult result = executionEngine.getResult(token);
            if (result.state() == CodeExecutionEngine.EngineState.FINISHED) {
                return result;
            }
            try {
                Thread.sleep(pollingInterval.toMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Worker was interrupted", exception);
            }
        }
        throw new IllegalStateException("Judge0 did not finish execution before the timeout");
    }

    private String diagnostic(CodeExecutionEngine.ExecutionResult result) {
        String value = result.verdict() == CodeExecutionEngine.Verdict.COMPILE_ERROR
                ? result.compileOutput()
                : result.stderr() != null ? result.stderr() : result.message();
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, Math.min(value.length(), 4_000));
    }
}
