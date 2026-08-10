package edu.ai.lms.core.coderunner.app.domain;

import java.util.UUID;

public record CodeSubmissionTestResult(
        UUID id,
        UUID submissionId,
        UUID testCaseId,
        String externalTestId,
        String visibility,
        String executionStatus,
        String verdict,
        double pointsEarned,
        Double timeMs,
        Long memoryKb,
        String diagnostic,
        String engineSubmissionId) {
}
