package edu.ai.lms.core.attempt.app.domain;

import java.util.List;
import java.util.UUID;

public record AttemptCodeRunnerQuestion(UUID questionId, String title, String statement, String starterCode,
                                        String entryPoint, String executionProfile, double totalPoints, int timeLimitMs,
                                        int memoryLimitKb, UUID activeSubmissionId,
                                        UUID latestSubmitId, UUID latestCompletedSubmitId, List<PublicTest> publicTests) {

    public AttemptCodeRunnerQuestion(UUID questionId, String title, String statement, String starterCode,
                                     String entryPoint, String executionProfile, UUID activeSubmissionId,
                                     UUID latestSubmitId, List<PublicTest> publicTests) {
        this(questionId, title, statement, starterCode, entryPoint, executionProfile,
                0, 5_000, 131_072, activeSubmissionId, latestSubmitId, null, publicTests);
    }
    public record PublicTest(String id, String category, String argumentsJson, String expectedOutputJson) { }
}
