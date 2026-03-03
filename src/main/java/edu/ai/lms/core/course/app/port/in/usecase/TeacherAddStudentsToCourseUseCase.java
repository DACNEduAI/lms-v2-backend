package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.AddStudentsToCourseResponse;
import edu.ai.lms.core.course.app.port.in.command.AddStudentsToCourseCommand;

public interface TeacherAddStudentsToCourseUseCase {
    AddStudentsToCourseResponse addStudentsToCourse(AddStudentsToCourseCommand command);
}
