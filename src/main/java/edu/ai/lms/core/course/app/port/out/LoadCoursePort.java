package edu.ai.lms.core.course.app.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.Course;

public interface LoadCoursePort {
    Optional<Course> findById(UUID courseId);
    Optional<Course> findByCourseCode(String courseCode);
    List<Course> findAllByTeacherId(UUID teacherId);
}
