package edu.ai.lms.core.coderunner.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.coderunner.api.CodeRunnerAcceptedView;
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
import edu.ai.lms.core.coderunner.app.port.out.CodeSubmissionPersistencePort;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerContractPort;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerQuestionContextPort;
import lombok.RequiredArgsConstructor;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerRateLimitException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class StudentCodeRunnerService {
    private static final int MAX_SOURCE_LENGTH = 100_000;

    private final LoadCodeRunnerQuestionContextPort questionContextPort;
    private final LoadCodeRunnerContractPort contractPort;
    private final CodeSubmissionPersistencePort submissionPort;
    private final ObjectMapper objectMapper;
    private final Clock clock;

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
                submissionPort.findLatestSubmissionId(attemptId, questionId).orElse(null),
                tests);
    }

    @Transactional
    public CodeRunnerAcceptedView execute(
            UUID attemptId, UUID studentId, UUID quizId, UUID questionId,
            String sourceCode, String idempotencyKey, CodeSubmission.Mode mode) {
        requireInput(sourceCode, idempotencyKey);
        requireQuestion(quizId, questionId);
        StoredCodeRunnerContract stored = requireExecutableContract(attemptId, questionId);
        rejectBannedApis(sourceCode, stored.contract());

        submissionPort.lockAttempt(attemptId);

        Optional<CodeSubmission> prior = submissionPort.findByStudentAndIdempotencyKey(studentId, idempotencyKey);
        if (prior.isPresent()) {
            CodeSubmission existing = prior.get();
            if (!existing.attemptId().equals(attemptId) || !existing.questionId().equals(questionId)
                    || existing.mode() != mode || !existing.sourceCode().equals(sourceCode)) {
                throw new CodeRunnerConflictException("Idempotency key was already used for another code request");
            }
            return new CodeRunnerAcceptedView(existing.id());
        }

        long cooldownSeconds = mode == CodeSubmission.Mode.SUBMIT ? 10 : 5;
        submissionPort.findLatestAcceptedAt(attemptId, questionId, mode.name())
            .ifPresent(lastAccepted -> {
                long elapsed = Duration.between(lastAccepted, clock.instant()).getSeconds();
                if (elapsed < cooldownSeconds) {
                    throw new CodeRunnerRateLimitException(cooldownSeconds - elapsed);
                }
            });

        List<ExecutableAssessmentContract.TestCase> tests = selectedTests(stored.contract(), mode);
        double maxScore = mode == CodeSubmission.Mode.SUBMIT
                ? tests.stream().mapToDouble(ExecutableAssessmentContract.TestCase::points).sum() : 0;
        UUID submissionId = UUID.randomUUID();
        CodeSubmission submission = new CodeSubmission(submissionId, questionId, stored.configId(), attemptId,
                studentId, sourceCode, idempotencyKey, mode, CodeSubmission.Status.PENDING,
                null, 0, maxScore, clock.instant(), null, null, 0, null);
        submissionPort.insert(submission);

        return new CodeRunnerAcceptedView(submissionId);
    }

    @Transactional(readOnly = true)
    public CodeRunnerSubmissionView getSubmission(UUID studentId, UUID submissionId, boolean hidesPrivateGrading) {
        CodeSubmission submission = submissionPort.findById(submissionId)
                .orElseThrow(() -> new CodeRunnerNotFoundException("Submission not found"));
        if (!submission.studentId().equals(studentId)) {
            throw new CodeRunnerConflictException("Submission does not belong to the user");
        }
        List<CodeSubmissionTestResult> results = submissionPort.findTestResults(submissionId);
        return view(submission, results, hidesPrivateGrading);
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

    public OptionalDouble findLatestCompletedSubmitRatio(UUID attemptId, UUID questionId) {
        return submissionPort.findLatestCompletedSubmitRatio(attemptId, questionId);
    }

    public boolean hasPendingSubmissions(UUID attemptId) {
        return submissionPort.hasPendingSubmissions(attemptId);
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

    private CodeRunnerSubmissionView view(CodeSubmission submission, List<CodeSubmissionTestResult> results, boolean forceHidePrivate) {
        List<CodeRunnerSubmissionView.VisibleTestResult> visible = results.stream()
                .filter(result -> "PUBLIC".equals(result.visibility()))
                .map(result -> new CodeRunnerSubmissionView.VisibleTestResult(result.externalTestId(),
                        result.verdict(), result.timeMs(), result.memoryKb(), result.diagnostic())).toList();
        boolean hidesPrivateGrading = forceHidePrivate && submission.mode() == CodeSubmission.Mode.SUBMIT;
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

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException(exception); }
    }
}
