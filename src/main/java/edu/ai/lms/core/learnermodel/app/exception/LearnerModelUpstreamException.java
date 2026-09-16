package edu.ai.lms.core.learnermodel.app.exception;

public class LearnerModelUpstreamException extends RuntimeException {
    public enum Kind { CONTRACT, BAD_RESPONSE, UNAVAILABLE }

    private final Kind kind;

    public LearnerModelUpstreamException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
