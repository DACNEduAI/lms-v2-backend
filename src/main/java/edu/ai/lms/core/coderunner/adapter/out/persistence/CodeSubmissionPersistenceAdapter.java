package edu.ai.lms.core.coderunner.adapter.out.persistence;

import edu.ai.lms.core.coderunner.app.domain.CodeSubmission;
import edu.ai.lms.core.coderunner.app.domain.CodeSubmissionTestResult;
import edu.ai.lms.core.coderunner.app.port.out.CodeSubmissionPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class CodeSubmissionPersistenceAdapter implements CodeSubmissionPersistencePort {
    private final CodeSubmissionRepository repository;

    @Override
    public Optional<CodeSubmission> findByStudentAndIdempotencyKey(UUID studentId, String key) {
        return repository.findByStudentAndIdempotencyKey(studentId, key).map(this::submission);
    }

    @Override
    public Optional<CodeSubmission> findById(UUID id) {
        return repository.findById(id).map(this::submission);
    }

    @Override public void insert(CodeSubmission submission) { repository.insert(submission); }

    @Override
    public void complete(UUID id, String status, String verdict, double score) {
        if (repository.complete(id, status, verdict, score) == 0) {
            throw new IllegalStateException("Code submission no longer exists");
        }
    }

    @Override public void insertTestResult(CodeSubmissionTestResult result) { repository.insertTestResult(result); }

    @Override
    public List<CodeSubmissionTestResult> findTestResults(UUID id) {
        return repository.findTestResults(id).stream().map(this::testResult).toList();
    }

    private CodeSubmission submission(CodeSubmissionRow row) {
        return new CodeSubmission(row.getId(), row.getQuestionId(), row.getConfigId(), row.getAttemptId(),
                row.getStudentId(), row.getSourceCode(), row.getIdempotencyKey(),
                CodeSubmission.Mode.valueOf(row.getMode()), CodeSubmission.Status.valueOf(row.getStatus()),
                row.getVerdict(), row.getScore(), row.getMaxScore(), row.getCreatedAt(), row.getCompletedAt());
    }

    private CodeSubmissionTestResult testResult(CodeSubmissionTestResultRow row) {
        return new CodeSubmissionTestResult(row.getId(), row.getSubmissionId(), row.getTestCaseId(),
                row.getExternalTestId(), row.getVisibility(), row.getExecutionStatus(), row.getVerdict(),
                row.getPointsEarned(), row.getTimeMs(), row.getMemoryKb(), row.getDiagnostic(),
                row.getEngineSubmissionId());
    }
}
