package edu.ai.lms.core.quiz.app.exception;

public class QuestionVersionConflictException extends RuntimeException {
    public QuestionVersionConflictException(String message) {
        super(message);
    }
}
