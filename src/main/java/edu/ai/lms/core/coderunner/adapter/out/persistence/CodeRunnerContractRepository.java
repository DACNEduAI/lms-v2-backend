package edu.ai.lms.core.coderunner.adapter.out.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
interface CodeRunnerContractRepository {

    int nextRevision(@Param("questionId") UUID questionId);

    void insertConfig(
            @Param("id") UUID id,
            @Param("questionId") UUID questionId,
            @Param("schemaVersion") String schemaVersion,
            @Param("contractId") String contractId,
            @Param("contractVersion") String contractVersion,
            @Param("contractStatus") String contractStatus,
            @Param("title") String title,
            @Param("statement") String statement,
            @Param("executionProfileId") String executionProfileId,
            @Param("executionProfileVersion") String executionProfileVersion,
            @Param("archetypeId") String archetypeId,
            @Param("archetypeVersion") String archetypeVersion,
            @Param("comparatorId") String comparatorId,
            @Param("comparatorVersion") String comparatorVersion,
            @Param("entryPoint") String entryPoint,
            @Param("signatureJson") String signatureJson,
            @Param("starterCode") String starterCode,
            @Param("bannedApiPatternsJson") String bannedApiPatternsJson,
            @Param("referenceSolution") String referenceSolution,
            @Param("revision") int revision);

    void insertTestCase(
            @Param("id") UUID id,
            @Param("configId") UUID configId,
            @Param("externalTestId") String externalTestId,
            @Param("category") String category,
            @Param("visibility") String visibility,
            @Param("argumentsJson") String argumentsJson,
            @Param("expectedOutputJson") String expectedOutputJson,
            @Param("points") double points,
            @Param("ordinal") int ordinal);

    Optional<CodeRunnerConfigRow> findLatestConfig(@Param("questionId") UUID questionId);

    Optional<CodeRunnerConfigRow> findConfigById(@Param("id") UUID id);

    Optional<CodeRunnerConfigRow> findReleasedConfigForAttempt(
            @Param("attemptId") UUID attemptId,
            @Param("questionId") UUID questionId);

    List<CodeRunnerTestCaseRow> findTestCases(@Param("configId") UUID configId);

    int markVerified(
            @Param("questionId") UUID questionId,
            @Param("configId") UUID configId);

    void insertVerification(
            @Param("id") UUID id,
            @Param("revisionId") UUID revisionId,
            @Param("verifiedBy") UUID verifiedBy,
            @Param("verifiedAt") java.time.Instant verifiedAt,
            @Param("executionProfile") String executionProfile,
            @Param("judge0SubmissionId") String judge0SubmissionId,
            @Param("verdict") String verdict,
            @Param("stdout") String stdout,
            @Param("stderr") String stderr,
            @Param("timeMs") Integer timeMs,
            @Param("memoryKb") Integer memoryKb,
            @Param("wallTimeLimitMs") Integer wallTimeLimitMs,
            @Param("memoryLimitKb") Integer memoryLimitKb);

    void insertVerificationCase(
            @Param("id") UUID id,
            @Param("verificationId") UUID verificationId,
            @Param("externalTestId") String externalTestId,
            @Param("visibility") String visibility,
            @Param("verdict") String verdict,
            @Param("timeMs") Double timeMs,
            @Param("memoryKb") Long memoryKb,
            @Param("diagnostic") String diagnostic);
}
