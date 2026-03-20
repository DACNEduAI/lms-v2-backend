package edu.ai.lms.core.course.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.course.adapter.in.response.GetCourseEnrollmentsResponse;

public interface TeacherGetCourseEnrollmentsUseCase {

    GetCourseEnrollmentsResponse getCourseEnrollments(UUID courseId);
}
