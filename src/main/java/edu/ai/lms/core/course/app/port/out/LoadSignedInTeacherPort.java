package edu.ai.lms.core.course.app.port.out;

import java.util.Optional;

import edu.ai.lms.core.course.app.domain.Teacher;

public interface LoadSignedInTeacherPort {
    Optional<Teacher> findAuthenticatedTeacher();
}
