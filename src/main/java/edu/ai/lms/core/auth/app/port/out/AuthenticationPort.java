package edu.ai.lms.core.auth.app.port.out;

import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.domain.Teacher;
import edu.ai.lms.core.auth.app.exception.InvalidCredentialsException;

/**
 * Port for authenticating users.
 * This port abstracts the authentication mechanism from the application layer.
 */
public interface AuthenticationPort {
    
    /**
     * Authenticates a student with the provided credentials.
     *
     * @param email    the student's email
     * @param password the student's password
     * @return the authenticated Student domain object
     * @throws InvalidCredentialsException if authentication fails
     */
    Student authenticateStudent(String email, String password);

    /**
     * Authenticates a teacher with the provided credentials.
     *
     * @param email    the teacher's email
     * @param password the teacher's password
     * @return the authenticated Teacher domain object
     * @throws InvalidCredentialsException if authentication fails
     */
    Teacher authenticateTeacher(String email, String password);
}
