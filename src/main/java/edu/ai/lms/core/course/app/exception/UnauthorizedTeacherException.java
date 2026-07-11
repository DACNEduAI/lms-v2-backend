package edu.ai.lms.core.course.app.exception;

public class UnauthorizedTeacherException extends RuntimeException {
    public UnauthorizedTeacherException(String message) {
        super(message);
    }
}
