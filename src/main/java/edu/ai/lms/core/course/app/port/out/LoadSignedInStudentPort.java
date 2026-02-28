package edu.ai.lms.core.course.app.port.out;

import java.util.Optional;

import edu.ai.lms.core.course.app.domain.Student;

public interface LoadSignedInStudentPort {
    Optional<Student> findAuthenticatedStudent();
}
