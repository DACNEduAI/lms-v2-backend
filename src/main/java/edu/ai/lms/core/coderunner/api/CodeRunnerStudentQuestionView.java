package edu.ai.lms.core.coderunner.api;

import java.util.List;
import java.util.UUID;

public record CodeRunnerStudentQuestionView(
        UUID questionId,
        String title,
        String statement,
        String starterCode,
        String entryPoint,
        String executionProfile,
        double totalPoints,
        int timeLimitMs,
        int memoryLimitKb,
        UUID activeSubmissionId,
        UUID latestSubmitId,
        UUID latestCompletedSubmitId,
        List<PublicTestView> publicTests) {

    /** Compatibility constructor for test fixtures that predate student execution metadata. */
    public CodeRunnerStudentQuestionView(UUID questionId, String title, String statement, String starterCode,
                                         String entryPoint, String executionProfile, UUID activeSubmissionId,
                                         UUID latestSubmitId, List<PublicTestView> publicTests) {
        this(questionId, title, statement, starterCode, entryPoint, executionProfile,
                0, 5_000, 131_072, activeSubmissionId, latestSubmitId, null, publicTests);
    }

    public record PublicTestView(String id, String category, String argumentsJson, String expectedOutputJson) { }
}
