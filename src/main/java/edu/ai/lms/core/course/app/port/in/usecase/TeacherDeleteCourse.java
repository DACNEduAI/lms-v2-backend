package edu.ai.lms.core.course.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.course.adapter.in.response.DeleteCourseResponse;

public interface TeacherDeleteCourse {
    DeleteCourseResponse deleteCourse(UUID courseId);
}
