package edu.ai.lms.core.course.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.course.adapter.in.response.GetCourseResponse;

public interface GetCourseUseCase {
    GetCourseResponse getCourse(UUID courseId);
}
