package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.CreateCourseResponse;
import edu.ai.lms.core.course.app.port.in.command.CreateCourseCommand;

public interface TeacherCreateCourseUseCase {
    CreateCourseResponse createCourse(CreateCourseCommand command);
}
