package edu.ai.lms.core.course.app.port.out;

import edu.ai.lms.core.course.app.domain.Student;

import java.util.Optional;

public interface LoadSignedInStudentPort {
    Optional<Student> findAuthenticatedTeacher();
}
