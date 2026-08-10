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
    void insert(CodeSubmission submission);
    int complete(@Param("submissionId") UUID submissionId, @Param("status") String status,
                 @Param("verdict") String verdict, @Param("score") double score);
    void insertTestResult(CodeSubmissionTestResult result);
    List<CodeSubmissionTestResultRow> findTestResults(@Param("submissionId") UUID submissionId);
}
