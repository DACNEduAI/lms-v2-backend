package edu.ai.lms.core.coderunner.app.port.out;

import edu.ai.lms.core.coderunner.app.domain.CodeSubmission;
import edu.ai.lms.core.coderunner.app.domain.CodeSubmissionTestResult;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

public interface CodeSubmissionPersistencePort {

    Optional<CodeSubmission> findByStudentAndIdempotencyKey(UUID studentId, String idempotencyKey);

    Optional<CodeSubmission> findById(UUID submissionId);
    
    void lockAttempt(UUID attemptId);
    
    Optional<java.time.Instant> findLatestAcceptedAt(UUID attemptId, UUID questionId, String mode);

    Optional<CodeSubmission> acquireNextPendingSubmission();

    void insert(CodeSubmission submission);

    void complete(UUID submissionId, String status, String verdict, double score);

    void fail(UUID submissionId, String status, int attemptCount, java.time.Instant nextAttemptAt);

    void insertTestResult(CodeSubmissionTestResult result);

    List<CodeSubmissionTestResult> findTestResults(UUID submissionId);

    boolean hasPendingSubmissions(UUID attemptId);

    OptionalDouble findLatestCompletedSubmitRatio(UUID attemptId, UUID questionId);

    Optional<UUID> findLatestSubmissionId(UUID attemptId, UUID questionId);

    Optional<UUID> findLatestSubmitId(UUID attemptId, UUID questionId);
}
