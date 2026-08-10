package edu.ai.lms.core.coderunner.app.service;

import edu.ai.lms.core.coderunner.adapter.in.http.response.CodeRunnerVerificationResponse;
import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CodeRunnerVerificationService {

    private final CodeExecutionEngine executionEngine;
    private final CppCodeRunnerHarnessBuilder harnessBuilder;
    private final int cpp17LanguageId;
    private final Duration pollingTimeout;
    private final Duration pollingInterval;

    public CodeRunnerVerificationService(
            CodeExecutionEngine executionEngine,
            CppCodeRunnerHarnessBuilder harnessBuilder,
            @Value("${code-runner.profile.dsa-cpp17.language-id:54}") int cpp17LanguageId,
            @Value("${code-runner.verification.polling-timeout:45s}") Duration pollingTimeout,
            @Value("${code-runner.verification.polling-interval:300ms}") Duration pollingInterval) {
        this.executionEngine = executionEngine;
        this.harnessBuilder = harnessBuilder;
        this.cpp17LanguageId = cpp17LanguageId;
        this.pollingTimeout = pollingTimeout;
        this.pollingInterval = pollingInterval;
    }

    public CodeRunnerVerificationResponse verifyReference(ExecutableAssessmentContract contract) {
        contract.validateForPersistence();
        List<ExecutableAssessmentContract.TestCase> tests = new ArrayList<>(contract.runtime().publicTests());
        tests.addAll(contract.privateGrading().hiddenTests());

        List<CodeRunnerVerificationResponse.TestResult> results = new ArrayList<>();
        for (var test : tests) {
            String harness = harnessBuilder.buildCase(
                    contract,
                    contract.privateGrading().referenceSolution(),
                    test);
            CodeExecutionEngine.ExecutionTicket ticket = executionEngine.submit(
                    new CodeExecutionEngine.ExecutionRequest(
                            harness, cpp17LanguageId, null,
                            2_000, 5_000, 131_072, 64, 16));
            CodeExecutionEngine.ExecutionResult result = await(ticket.engineSubmissionId());
            CodeExecutionEngine.Verdict verdict = effectiveVerdict(
                    result, harnessBuilder.expectedOutput(contract, test));
            results.add(new CodeRunnerVerificationResponse.TestResult(
                    test.id(),
                    test.visibility(),
                    verdict.name(),
                    result.timeMs(),
                    result.memoryKb(),
                    diagnostic(result)));
            if (verdict == CodeExecutionEngine.Verdict.COMPILE_ERROR) {
                break;
            }
        }

        int passed = (int) results.stream()
                .filter(result -> "ACCEPTED".equals(result.verdict()))
                .count();
        return new CodeRunnerVerificationResponse(
                results.size() == tests.size() && passed == tests.size(),
                passed,
                tests.size(),
                List.copyOf(results));
    }

    private CodeExecutionEngine.Verdict effectiveVerdict(
            CodeExecutionEngine.ExecutionResult result,
            String expectedOutput) {
        if (result.verdict() == null) {
            return CodeExecutionEngine.Verdict.INTERNAL_ERROR;
        }
        if (result.verdict() == CodeExecutionEngine.Verdict.ACCEPTED
                && !expectedOutput.equals(result.stdout())) {
            return CodeExecutionEngine.Verdict.WRONG_ANSWER;
        }
        return result.verdict();
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
                throw new IllegalStateException("Code Runner verification was interrupted", exception);
            }
        }
        throw new IllegalStateException("Judge0 did not finish verification before the timeout");
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
