package edu.ai.lms.core.course.app.port.out;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface LoadStudentsByEmailPort {

    /**
     * Finds student IDs for the given list of emails.
     * Returns a map of email → student UUID for all emails that exist in the system.
     *
     * @param emails list of student email addresses
     * @return map of email to student UUID for found students
     */
    Map<String, UUID> findStudentIdsByEmails(List<String> emails);
}
