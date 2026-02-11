package edu.ai.lms.core.course.exception;

public class CourseCodeAlreadyExistsException extends RuntimeException {
    public CourseCodeAlreadyExistsException(String message) {
        super(message);
    }
}
