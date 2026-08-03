package edu.ai.lms.core.coderunner.adapter.in.http.response;

import java.util.List;

public record CodeRunnerVerificationResponse(
        boolean passed,
        int passedTests,
        int totalTests,
        List<TestResult> results) {

    public record TestResult(
            String testId,
            String visibility,
            String verdict,
            Double timeMs,
            Long memoryKb,
            String diagnostic) {
    }
}
