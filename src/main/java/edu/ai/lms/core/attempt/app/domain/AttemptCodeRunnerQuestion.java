package edu.ai.lms.core.attempt.app.domain;

import java.util.List;
import java.util.UUID;

public record AttemptCodeRunnerQuestion(UUID questionId, String title, String statement, String starterCode,
                                        String entryPoint, String executionProfile, UUID activeSubmissionId,
                                        UUID latestSubmitId, List<PublicTest> publicTests) {
    public record PublicTest(String id, String category, String argumentsJson, String expectedOutputJson) { }
}
