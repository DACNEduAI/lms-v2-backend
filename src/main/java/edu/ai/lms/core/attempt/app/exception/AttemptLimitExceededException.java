package edu.ai.lms.core.attempt.app.exception;

public class AttemptLimitExceededException extends RuntimeException {

    public AttemptLimitExceededException(String message) {
        super(message);
    }
}
