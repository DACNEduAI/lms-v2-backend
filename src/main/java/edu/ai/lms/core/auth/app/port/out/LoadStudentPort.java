package edu.ai.lms.core.auth.app.port.out;

import java.util.Optional;

import edu.ai.lms.core.auth.app.domain.Student;

public interface LoadStudentPort {
    Optional<Student> findByEmail(String email);
}
