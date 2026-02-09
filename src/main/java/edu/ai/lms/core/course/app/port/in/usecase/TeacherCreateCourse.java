package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.request.CreateCourseRequest;
import edu.ai.lms.core.course.adapter.in.response.CreateCourseResponse;

public interface TeacherCreateCourse {
    public CreateCourseResponse createCourse(CreateCourseRequest request);
}
