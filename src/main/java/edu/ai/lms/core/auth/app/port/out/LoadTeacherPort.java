package edu.ai.lms.core.auth.app.port.out;

import java.util.Optional;

import edu.ai.lms.core.auth.app.domain.Teacher;

public interface LoadTeacherPort {
    Optional<Teacher> findByEmail(String email);
}
