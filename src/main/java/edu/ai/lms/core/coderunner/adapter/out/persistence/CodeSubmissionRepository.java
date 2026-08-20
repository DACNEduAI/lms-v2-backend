package edu.ai.lms.core.coderunner.adapter.out.persistence;

import edu.ai.lms.core.coderunner.app.domain.CodeSubmission;
import edu.ai.lms.core.coderunner.app.domain.CodeSubmissionTestResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
interface CodeSubmissionRepository {
    Optional<CodeSubmissionRow> findByStudentAndIdempotencyKey(
            @Param("studentId") UUID studentId, @Param("idempotencyKey") String idempotencyKey);
    Optional<CodeSubmissionRow> findById(@Param("submissionId") UUID submissionId);
    void lockAttempt(@Param("attemptId") UUID attemptId);
    Optional<java.time.Instant> findLatestAcceptedAt(@Param("attemptId") UUID attemptId,
                                        @Param("questionId") UUID questionId,
                                        @Param("mode") String mode);
    Optional<CodeSubmissionRow> acquireNextPendingSubmission();
    void insert(CodeSubmission submission);
    int complete(@Param("submissionId") UUID submissionId, @Param("status") String status,
                 @Param("verdict") String verdict, @Param("score") double score);
    int fail(@Param("submissionId") UUID submissionId, @Param("status") String status,
             @Param("attemptCount") int attemptCount, @Param("nextAttemptAt") java.time.Instant nextAttemptAt);
    void insertTestResult(CodeSubmissionTestResult result);
    List<CodeSubmissionTestResultRow> findTestResults(@Param("submissionId") UUID submissionId);
    long countPendingSubmissions(@Param("attemptId") UUID attemptId);
    Double findLatestCompletedSubmitRatio(@Param("attemptId") UUID attemptId, @Param("questionId") UUID questionId);
    UUID findLatestSubmissionId(@Param("attemptId") UUID attemptId, @Param("questionId") UUID questionId);

    UUID findLatestSubmitId(@Param("attemptId") UUID attemptId, @Param("questionId") UUID questionId);
}
