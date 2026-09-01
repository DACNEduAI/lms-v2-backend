package edu.ai.lms.core.coderunner.app.exception;

public class CodeRunnerAccessDeniedException extends RuntimeException {
    public CodeRunnerAccessDeniedException(String message) {
        super(message);
    }
}
