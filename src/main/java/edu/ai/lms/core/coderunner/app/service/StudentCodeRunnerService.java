package edu.ai.lms.core.coderunner.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.coderunner.api.CodeRunnerStudentQuestionView;
import edu.ai.lms.core.coderunner.api.CodeRunnerSubmissionScoreView;
import edu.ai.lms.core.coderunner.api.CodeRunnerSubmissionView;
import edu.ai.lms.core.coderunner.app.domain.CodeRunnerQuestionContext;
import edu.ai.lms.core.coderunner.app.domain.CodeSubmission;
import edu.ai.lms.core.coderunner.app.domain.CodeSubmissionTestResult;
import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import edu.ai.lms.core.coderunner.app.domain.StoredCodeRunnerContract;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerConflictException;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerNotFoundException;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine;
import edu.ai.lms.core.coderunner.app.port.out.CodeSubmissionPersistencePort;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerContractPort;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerQuestionContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class StudentCodeRunnerService {
    private static final int MAX_SOURCE_LENGTH = 100_000;

    private final LoadCodeRunnerQuestionContextPort questionContextPort;
    private final LoadCodeRunnerContractPort contractPort;
    private final CodeSubmissionPersistencePort submissionPort;
    private final CodeExecutionEngine executionEngine;
    private final CppCodeRunnerHarnessBuilder harnessBuilder;
    private final ObjectMapper objectMapper;
    @Value("${code-runner.profile.dsa-cpp17.language-id:54}") private int cpp17LanguageId;
    @Value("${code-runner.verification.polling-timeout:45s}") private Duration pollingTimeout;
    @Value("${code-runner.verification.polling-interval:300ms}") private Duration pollingInterval;

    public CodeRunnerStudentQuestionView getQuestion(UUID attemptId, UUID quizId, UUID questionId) {
        requireQuestion(quizId, questionId);
        StoredCodeRunnerContract stored = requireExecutableContract(attemptId, questionId);
        var contract = stored.contract();
        List<CodeRunnerStudentQuestionView.PublicTestView> tests = contract.runtime().publicTests().stream()
                .map(test -> new CodeRunnerStudentQuestionView.PublicTestView(
                        test.id(), test.category(), json(test.arguments()), json(test.expectedOutput())))
                .toList();
        return new CodeRunnerStudentQuestionView(questionId, contract.title(), contract.statement(),
                contract.runtime().starterCode(), contract.runtime().entryPoint(),
                contract.runtime().executionProfile().id() + ":" + contract.runtime().executionProfile().version(),
                tests);
    }

    public CodeRunnerSubmissionView execute(
            UUID attemptId, UUID studentId, UUID quizId, UUID questionId,
            String sourceCode, String idempotencyKey, CodeSubmission.Mode mode) {
        requireInput(sourceCode, idempotencyKey);
        requireQuestion(quizId, questionId);
        StoredCodeRunnerContract stored = requireExecutableContract(attemptId, questionId);
        rejectBannedApis(sourceCode, stored.contract());

        Optional<CodeSubmission> prior = submissionPort.findByStudentAndIdempotencyKey(studentId, idempotencyKey);
        if (prior.isPresent()) {
            CodeSubmission existing = prior.get();
            if (!existing.attemptId().equals(attemptId) || !existing.questionId().equals(questionId)
                    || existing.mode() != mode || !existing.sourceCode().equals(sourceCode)) {
                throw new CodeRunnerConflictException("Idempotency key was already used for another code request");
            }
            return view(existing, submissionPort.findTestResults(existing.id()));
        }

        List<ExecutableAssessmentContract.TestCase> tests = selectedTests(stored.contract(), mode);
        double maxScore = mode == CodeSubmission.Mode.SUBMIT
                ? tests.stream().mapToDouble(ExecutableAssessmentContract.TestCase::points).sum() : 0;
        UUID submissionId = UUID.randomUUID();
        CodeSubmission submission = new CodeSubmission(submissionId, questionId, stored.configId(), attemptId,
                studentId, sourceCode, idempotencyKey, mode, CodeSubmission.Status.PROCESSING,
                null, 0, maxScore, null, null);
        submissionPort.insert(submission);

        List<CodeSubmissionTestResult> results = new ArrayList<>();
        try {
            for (var test : tests) {
                String harness = harnessBuilder.buildCase(stored.contract(), sourceCode, test);
                var ticket = executionEngine.submit(new CodeExecutionEngine.ExecutionRequest(
                        harness, cpp17LanguageId, null, 2_000, 5_000, 131_072, 64, 16));
                var engineResult = await(ticket.engineSubmissionId());
                CodeExecutionEngine.Verdict effectiveVerdict = effectiveVerdict(
                        engineResult, harnessBuilder.expectedOutput(stored.contract(), test));
                String verdict = effectiveVerdict.name();
                double earned = mode == CodeSubmission.Mode.SUBMIT && "ACCEPTED".equals(verdict)
                        ? test.points() : 0;
                CodeSubmissionTestResult result = new CodeSubmissionTestResult(
                        UUID.randomUUID(), submissionId, requiredTestId(stored, test.id()), test.id(),
                        test.visibility(), "COMPLETED", verdict, earned, engineResult.timeMs(),
                        engineResult.memoryKb(), diagnostic(engineResult), ticket.engineSubmissionId());
                submissionPort.insertTestResult(result);
                results.add(result);
                if (effectiveVerdict == CodeExecutionEngine.Verdict.COMPILE_ERROR
                        || effectiveVerdict == CodeExecutionEngine.Verdict.INTERNAL_ERROR) break;
            }
            double score = results.stream().mapToDouble(CodeSubmissionTestResult::pointsEarned).sum();
            String verdict = aggregateVerdict(results, tests.size());
            submissionPort.complete(submissionId, "COMPLETED", verdict, score);
            return view(submissionPort.findById(submissionId).orElseThrow(), results);
        } catch (RuntimeException exception) {
            submissionPort.complete(submissionId, "FAILED", "INTERNAL_ERROR", 0);
            throw exception;
        }
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

    public Optional<CodeRunnerSubmissionScoreView> findCompletedSubmit(
            UUID submissionId, UUID attemptId, UUID studentId, UUID questionId) {
        return submissionPort.findById(submissionId)
                .filter(item -> item.attemptId().equals(attemptId)
                        && item.studentId().equals(studentId)
                        && item.questionId().equals(questionId)
                        && item.mode() == CodeSubmission.Mode.SUBMIT
                        && item.status() == CodeSubmission.Status.COMPLETED)
                .map(item -> new CodeRunnerSubmissionScoreView(
                        item.id(), item.questionId(), item.score(), item.maxScore()));
    }

    private void requireQuestion(UUID quizId, UUID questionId) {
        CodeRunnerQuestionContext context = questionContextPort.findQuestionContext(questionId)
                .orElseThrow(() -> new CodeRunnerNotFoundException("Question not found"));
        if (!quizId.equals(context.quizId()) || !"CODE_RUNNER".equals(context.questionType())) {
            throw new CodeRunnerNotFoundException("Code Runner question not found in this quiz");
        }
    }

    private StoredCodeRunnerContract requireExecutableContract(UUID attemptId, UUID questionId) {
        StoredCodeRunnerContract stored = contractPort.findReleasedForAttempt(attemptId, questionId)
                .orElseThrow(() -> new CodeRunnerNotFoundException(
                        "Code Runner contract is not bound to this attempt release"));
        if (!"VERIFIED_EXECUTION_GOLD".equals(stored.contract().status())) {
            throw new CodeRunnerConflictException("Code Runner contract has not passed executable verification");
        }
        return stored;
    }

    private List<ExecutableAssessmentContract.TestCase> selectedTests(
            ExecutableAssessmentContract contract, CodeSubmission.Mode mode) {
        List<ExecutableAssessmentContract.TestCase> tests = new ArrayList<>(contract.runtime().publicTests());
        if (mode == CodeSubmission.Mode.SUBMIT) tests.addAll(contract.privateGrading().hiddenTests());
        return tests;
    }

    private UUID requiredTestId(StoredCodeRunnerContract stored, String externalId) {
        UUID id = stored.testCaseIds().get(externalId);
        if (id == null) throw new IllegalStateException("Stored test case id is missing: " + externalId);
        return id;
    }

    private void requireInput(String source, String key) {
        if (source == null || source.isBlank() || source.length() > MAX_SOURCE_LENGTH) {
            throw new IllegalArgumentException("sourceCode must contain between 1 and 100000 characters");
        }
        if (key == null || key.isBlank() || key.length() > 255) {
            throw new IllegalArgumentException("Idempotency-Key must contain between 1 and 255 characters");
        }
    }

    private void rejectBannedApis(String source, ExecutableAssessmentContract contract) {
        contract.runtime().bannedApiPatterns().stream().map(Pattern::compile)
                .filter(pattern -> pattern.matcher(source).find()).findFirst()
                .ifPresent(pattern -> { throw new IllegalArgumentException("Source code uses a banned API"); });
    }

    private CodeExecutionEngine.ExecutionResult await(String token) {
        Instant deadline = Instant.now().plus(pollingTimeout);
        while (Instant.now().isBefore(deadline)) {
            var result = executionEngine.getResult(token);
            if (result.state() == CodeExecutionEngine.EngineState.FINISHED) return result;
            try { Thread.sleep(pollingInterval.toMillis()); }
            catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Code execution was interrupted", exception);
            }
        }
        throw new IllegalStateException("Judge0 did not finish before the timeout");
    }

    private String aggregateVerdict(List<CodeSubmissionTestResult> results, int intendedTests) {
        if (results.size() == intendedTests && results.stream().allMatch(r -> "ACCEPTED".equals(r.verdict()))) {
            return "ACCEPTED";
        }
        return results.stream().filter(r -> !"ACCEPTED".equals(r.verdict()))
                .map(CodeSubmissionTestResult::verdict).findFirst().orElse("INTERNAL_ERROR");
    }

    private CodeRunnerSubmissionView view(CodeSubmission submission, List<CodeSubmissionTestResult> results) {
        List<CodeRunnerSubmissionView.VisibleTestResult> visible = results.stream()
                .filter(result -> "PUBLIC".equals(result.visibility()))
                .map(result -> new CodeRunnerSubmissionView.VisibleTestResult(result.externalTestId(),
                        result.verdict(), result.timeMs(), result.memoryKb(), result.diagnostic())).toList();
        boolean hidesPrivateGrading = submission.mode() == CodeSubmission.Mode.SUBMIT;
        int visiblePassed = (int) results.stream()
                .filter(result -> "PUBLIC".equals(result.visibility()))
                .filter(result -> "ACCEPTED".equals(result.verdict()))
                .count();
        return new CodeRunnerSubmissionView(submission.id(), submission.questionId(), submission.mode().name(),
                submission.status().name(), hidesPrivateGrading ? null : submission.verdict(),
                hidesPrivateGrading ? null : submission.score(),
                hidesPrivateGrading ? null : submission.maxScore(),
                hidesPrivateGrading ? null : visiblePassed,
                hidesPrivateGrading ? null : visible.size(), visible);
    }

    private String diagnostic(CodeExecutionEngine.ExecutionResult result) {
        String value = result.verdict() == CodeExecutionEngine.Verdict.COMPILE_ERROR
                ? result.compileOutput() : result.stderr() != null ? result.stderr() : result.message();
        return value == null ? "" : value.substring(0, Math.min(4_000, value.length()));
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException(exception); }
    }
}
