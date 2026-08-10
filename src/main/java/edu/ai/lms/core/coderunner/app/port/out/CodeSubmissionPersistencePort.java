package edu.ai.lms.core.coderunner.app.port.out;

import edu.ai.lms.core.coderunner.app.domain.CodeSubmission;
import edu.ai.lms.core.coderunner.app.domain.CodeSubmissionTestResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CodeSubmissionPersistencePort {

    Optional<CodeSubmission> findByStudentAndIdempotencyKey(UUID studentId, String idempotencyKey);

    Optional<CodeSubmission> findById(UUID submissionId);

    void insert(CodeSubmission submission);

    void complete(UUID submissionId, String status, String verdict, double score);

    void insertTestResult(CodeSubmissionTestResult result);

    List<CodeSubmissionTestResult> findTestResults(UUID submissionId);
}
