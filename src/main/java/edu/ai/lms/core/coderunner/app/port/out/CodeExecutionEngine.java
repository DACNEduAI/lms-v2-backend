package edu.ai.lms.core.coderunner.app.port.out;

public interface CodeExecutionEngine {

    ExecutionTicket submit(ExecutionRequest request);

    ExecutionResult getResult(String engineSubmissionId);

    record ExecutionRequest(
            String sourceCode,
            int languageId,
            String expectedOutput,
            long cpuTimeMs,
            long wallTimeMs,
            long memoryKb,
            long outputLimitKb,
            int maxProcessesAndThreads) {
    }

    record ExecutionTicket(String engineSubmissionId) {
    }

    record ExecutionResult(
            EngineState state,
            Verdict verdict,
            String stdout,
            String compileOutput,
            String stderr,
            Double timeMs,
            Long memoryKb,
            String message) {
    }

    enum EngineState {
        QUEUED,
        PROCESSING,
        FINISHED
    }

    enum Verdict {
        ACCEPTED,
        WRONG_ANSWER,
        TIME_LIMIT_EXCEEDED,
        COMPILE_ERROR,
        RUNTIME_ERROR,
        OUTPUT_LIMIT_EXCEEDED,
        INTERNAL_ERROR
    }
}
