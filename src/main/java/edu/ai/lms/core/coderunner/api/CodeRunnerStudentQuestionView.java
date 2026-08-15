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
        UUID activeSubmissionId,
        List<PublicTestView> publicTests) {

    public record PublicTestView(String id, String category, String argumentsJson, String expectedOutputJson) { }
}
