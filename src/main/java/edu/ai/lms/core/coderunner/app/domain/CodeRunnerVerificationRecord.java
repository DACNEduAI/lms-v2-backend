package edu.ai.lms.core.coderunner.app.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CodeRunnerVerificationRecord(
        UUID id,
        UUID revisionId,
        UUID verifiedBy,
        Instant verifiedAt,
        String executionProfileJson,
        String judge0SubmissionId,
        String verdict,
        String stdout,
        String stderr,
        Integer timeMs,
        Integer memoryKb,
        Integer wallTimeLimitMs,
        Integer memoryLimitKb,
        List<TestCaseResult> testCaseResults
) {
    public record TestCaseResult(
            String externalTestId,
            String visibility,
            String verdict,
            Double timeMs,
            Long memoryKb,
            String diagnostic
    ) {}
}
