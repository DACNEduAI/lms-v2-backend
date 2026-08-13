package edu.ai.lms.core.attempt.app.domain;

import java.util.List;
import java.util.UUID;

public record AttemptCodeSubmission(UUID submissionId, UUID questionId, String mode, String status,
                                    String verdict, Double score, Double maxScore, Integer passedTests,
                                    Integer totalTests, List<VisibleTestResult> visibleResults) {
    public record VisibleTestResult(String testId, String verdict, Double timeMs, Long memoryKb, String diagnostic) { }
}
