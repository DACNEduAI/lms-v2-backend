package edu.ai.lms.core.coderunner.api;

import java.util.UUID;

public record CodeRunnerSubmissionScoreView(
        UUID submissionId,
        UUID questionId,
        double score,
        double maxScore) {

    public double ratio() {
        return maxScore <= 0 ? 0 : Math.max(0, Math.min(1, score / maxScore));
    }
}
