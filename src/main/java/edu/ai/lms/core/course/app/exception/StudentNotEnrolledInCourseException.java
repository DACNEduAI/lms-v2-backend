package edu.ai.lms.core.course.app.exception;

import java.util.List;

public class StudentNotEnrolledInCourseException extends RuntimeException {

    private final List<String> notEnrolledEmails;

    public StudentNotEnrolledInCourseException(List<String> notEnrolledEmails) {
        super("The following students are not enrolled in this course: " + notEnrolledEmails);
        this.notEnrolledEmails = notEnrolledEmails;
    }

    public List<String> getNotEnrolledEmails() {
        return notEnrolledEmails;
    }
}
