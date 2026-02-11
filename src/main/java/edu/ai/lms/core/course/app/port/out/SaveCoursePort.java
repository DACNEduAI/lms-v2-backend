package edu.ai.lms.core.course.app.port.out;

import edu.ai.lms.core.course.app.domain.Course;

public interface SaveCoursePort {
    Course save(Course course);
}
