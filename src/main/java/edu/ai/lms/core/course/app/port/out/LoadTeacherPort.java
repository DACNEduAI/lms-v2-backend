package edu.ai.lms.core.course.app.port.out;

import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.Teacher;

public interface LoadTeacherPort {
    Optional<Teacher> findById(UUID teacherId);
}
