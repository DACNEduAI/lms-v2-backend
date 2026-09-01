package edu.ai.lms.core.coderunner.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.coderunner.adapter.in.http.response.CodeRunnerVerificationResponse;
import edu.ai.lms.core.coderunner.app.domain.CodeRunnerVerificationRecord;
import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine;
import edu.ai.lms.core.coderunner.app.port.out.SaveCodeRunnerContractPort;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerConflictException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CodeRunnerVerificationService {

    private final CodeExecutionEngine executionEngine;
    private final CppCodeRunnerHarnessBuilder harnessBuilder;
    private final SaveCodeRunnerContractPort saveContractPort;
    private final ObjectMapper objectMapper;
    private final int cpp17LanguageId;
    private final Duration pollingTimeout;
    private final Duration pollingInterval;

    public CodeRunnerVerificationService(
            CodeExecutionEngine executionEngine,
            CppCodeRunnerHarnessBuilder harnessBuilder,
            SaveCodeRunnerContractPort saveContractPort,
            ObjectMapper objectMapper,
            @Value("${code-runner.profile.dsa-cpp17.language-id:54}") int cpp17LanguageId,
            @Value("${code-runner.verification.polling-timeout:45s}") Duration pollingTimeout,
            @Value("${code-runner.verification.polling-interval:300ms}") Duration pollingInterval) {
        this.executionEngine = executionEngine;
        this.harnessBuilder = harnessBuilder;
        this.saveContractPort = saveContractPort;
        this.objectMapper = objectMapper;
        this.cpp17LanguageId = cpp17LanguageId;
        this.pollingTimeout = pollingTimeout;
        this.pollingInterval = pollingInterval;
    }

    public CodeRunnerVerificationResponse verifyReference(
            UUID configId, 
            UUID questionId,
            UUID verifiedBy,
            ExecutableAssessmentContract contract) {
        contract.validateForPersistence();
        List<ExecutableAssessmentContract.TestCase> tests = new ArrayList<>(contract.runtime().publicTests());
        tests.addAll(contract.privateGrading().hiddenTests());

        List<CodeRunnerVerificationResponse.TestResult> results = new ArrayList<>();
        
        String lastSubmissionId = null;
        CodeExecutionEngine.Verdict overallVerdict = CodeExecutionEngine.Verdict.ACCEPTED;
        String lastStdout = null;
        String lastStderr = null;
        int maxTimeMs = 0;
        int maxMemoryKb = 0;

        var limits = contract.effectiveExecutionLimits();
        int wallTimeLimitMs = limits.wallTimeLimitMs();
        int memoryLimitKb = limits.memoryLimitKb();

        for (var test : tests) {
            String harness = harnessBuilder.buildCase(
                    contract,
                    contract.privateGrading().referenceSolution(),
                    test);
            CodeExecutionEngine.ExecutionTicket ticket = executionEngine.submit(
                    new CodeExecutionEngine.ExecutionRequest(
                            harness, cpp17LanguageId, null,
                            2_000, wallTimeLimitMs, memoryLimitKb, 64, 16));
            lastSubmissionId = ticket.engineSubmissionId();
            
            CodeExecutionEngine.ExecutionResult result = await(ticket.engineSubmissionId());
            CodeExecutionEngine.Verdict verdict = effectiveVerdict(
                    result, harnessBuilder.expectedOutput(contract, test));
            
            lastStdout = result.stdout();
            lastStderr = result.stderr();
            if (result.timeMs() != null) maxTimeMs = Math.max(maxTimeMs, (int) Math.round(result.timeMs()));
            if (result.memoryKb() != null) maxMemoryKb = Math.max(maxMemoryKb, Math.toIntExact(result.memoryKb()));

            results.add(new CodeRunnerVerificationResponse.TestResult(
                    test.id(),
                    test.visibility(),
                    verdict.name(),
                    result.timeMs(),
                    result.memoryKb(),
                    diagnostic(result)));
            if (verdict != CodeExecutionEngine.Verdict.ACCEPTED) {
                overallVerdict = verdict;
                break;
            }
        }

        int passed = (int) results.stream()
                .filter(result -> "ACCEPTED".equals(result.verdict()))
                .count();
        
        boolean passedAll = results.size() == tests.size() && passed == tests.size();
        
        List<CodeRunnerVerificationRecord.TestCaseResult> recordTestCaseResults = results.stream()
                .map(r -> new CodeRunnerVerificationRecord.TestCaseResult(
                        r.testId(), r.visibility(), r.verdict(), r.timeMs(), r.memoryKb(), r.diagnostic()))
                .toList();

        if (configId != null) {
            try {
                CodeRunnerVerificationRecord record = new CodeRunnerVerificationRecord(
                        UUID.randomUUID(),
                        configId,
                        verifiedBy,
                        Instant.now(),
                        objectMapper.writeValueAsString(contract.runtime().executionProfile()),
                        lastSubmissionId,
                        overallVerdict.name(),
                        lastStdout,
                        lastStderr,
                        maxTimeMs,
                        maxMemoryKb,
                        wallTimeLimitMs,
                        memoryLimitKb,
                        recordTestCaseResults
                );
                saveContractPort.saveVerification(record);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize execution profile", e);
            }

            if (passedAll) {
                if (!saveContractPort.markVerified(questionId, configId)) {
                    throw new CodeRunnerConflictException(
                            "Code Runner contract changed during verification; verify the latest revision again");
                }
            }
        }

        return new CodeRunnerVerificationResponse(
                passedAll,
                passed,
                tests.size(),
                List.copyOf(results));
    }

    /** Executes a teacher's draft code without persisting a verification result. */
    public CodeRunnerVerificationResponse tryCandidate(
            ExecutableAssessmentContract contract,
            String sourceCode,
            boolean includeHidden) {
        contract.validateForPersistence();
        List<ExecutableAssessmentContract.TestCase> tests = new ArrayList<>(contract.runtime().publicTests());
        if (includeHidden) {
            tests.addAll(contract.privateGrading().hiddenTests());
        }

        List<CodeRunnerVerificationResponse.TestResult> results = new ArrayList<>();
        CodeExecutionEngine.Verdict overallVerdict = CodeExecutionEngine.Verdict.ACCEPTED;
        int passed = 0;
        var limits = contract.effectiveExecutionLimits();
        for (var test : tests) {
            String harness = harnessBuilder.buildCase(contract, sourceCode, test);
            CodeExecutionEngine.ExecutionTicket ticket = executionEngine.submit(
                    new CodeExecutionEngine.ExecutionRequest(
                            harness, cpp17LanguageId, null,
                            2_000, limits.wallTimeLimitMs(), limits.memoryLimitKb(), 64, 16));
            CodeExecutionEngine.ExecutionResult result = await(ticket.engineSubmissionId());
            CodeExecutionEngine.Verdict verdict = effectiveVerdict(
                    result, harnessBuilder.expectedOutput(contract, test));
            if (verdict == CodeExecutionEngine.Verdict.ACCEPTED) {
                passed++;
            } else {
                overallVerdict = verdict;
            }
            results.add(new CodeRunnerVerificationResponse.TestResult(
                    test.id(),
                    test.visibility(),
                    verdict.name(),
                    result.timeMs(),
                    result.memoryKb(),
                    diagnostic(result)));
        }
        return new CodeRunnerVerificationResponse(
                overallVerdict == CodeExecutionEngine.Verdict.ACCEPTED,
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
