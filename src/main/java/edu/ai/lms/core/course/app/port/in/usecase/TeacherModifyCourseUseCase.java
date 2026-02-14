package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.ModifyCourseResponse;
import edu.ai.lms.core.course.app.port.in.command.ModifyCourseCommand;

public interface TeacherModifyCourseUseCase {
    ModifyCourseResponse modifyCourse(ModifyCourseCommand command);
}
