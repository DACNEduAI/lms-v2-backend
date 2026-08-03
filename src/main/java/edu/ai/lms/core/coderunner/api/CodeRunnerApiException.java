package edu.ai.lms.core.coderunner.api;

public class CodeRunnerApiException extends RuntimeException {
    private final Kind kind;

    public CodeRunnerApiException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() { return kind; }

    public enum Kind { NOT_FOUND, CONFLICT, INVALID, EXECUTION_FAILED }
}
