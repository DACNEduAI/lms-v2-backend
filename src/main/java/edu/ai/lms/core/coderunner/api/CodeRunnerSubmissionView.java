package edu.ai.lms.core.coderunner.api;

import java.util.List;
import java.util.UUID;

public record CodeRunnerSubmissionView(
        UUID submissionId,
        UUID questionId,
        String mode,
        String status,
        String verdict,
        double score,
        double maxScore,
        int passedTests,
        int totalTests,
        List<VisibleTestResult> visibleResults) {

    public record VisibleTestResult(
            String testId, String verdict, Double timeMs, Long memoryKb, String diagnostic) { }
}
