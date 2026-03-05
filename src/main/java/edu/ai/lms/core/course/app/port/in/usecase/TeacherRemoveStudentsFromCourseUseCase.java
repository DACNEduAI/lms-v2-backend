package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.RemoveStudentsFromCourseResponse;
import edu.ai.lms.core.course.app.port.in.command.RemoveStudentsFromCourseCommand;

public interface TeacherRemoveStudentsFromCourseUseCase {
    RemoveStudentsFromCourseResponse removeStudentsFromCourse(RemoveStudentsFromCourseCommand command);
}
