package edu.ai.lms.core.coderunner.api;

import java.util.UUID;

public record CodeRunnerExecutionRequest(
        UUID attemptId,
        UUID studentId,
        UUID quizId,
        UUID questionId,
        String sourceCode,
        String idempotencyKey,
        Mode mode) {

    public enum Mode { RUN, SUBMIT }
}
