package edu.ai.lms.core.coderunner.app.exception;

public class CodeRunnerRateLimitException extends RuntimeException {
    private final long retryAfterSeconds;
    
    public CodeRunnerRateLimitException(long retryAfterSeconds) {
        super("Rate limit exceeded");
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
