package edu.ai.lms.core.course.app.exception;

import java.util.List;

public class StudentsNotFoundException extends RuntimeException {

    private final List<String> missingEmails;

    public StudentsNotFoundException(List<String> missingEmails) {
        super("The following students are not available in the system: " + missingEmails);
        this.missingEmails = missingEmails;
    }

    public List<String> getMissingEmails() {
        return missingEmails;
    }
}
