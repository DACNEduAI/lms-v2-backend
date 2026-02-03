package edu.ai.lms.core.auth.app.port.out;

import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.domain.Teacher;

/**
 * Port for generating authentication tokens.
 * This port abstracts the token generation mechanism from the application layer.
 */
public interface TokenGenerationPort {
    
    /**
     * Generates a JWT token for a Student.
     *
     * @param student the Student domain object
     * @return the generated JWT token
     */
    String generateStudentToken(Student student);
    
    /**
     * Generates a JWT token for a Teacher.
     *
     * @param teacher the Teacher domain object
     * @return the generated JWT token
     */
    String generateTeacherToken(Teacher teacher);
}
