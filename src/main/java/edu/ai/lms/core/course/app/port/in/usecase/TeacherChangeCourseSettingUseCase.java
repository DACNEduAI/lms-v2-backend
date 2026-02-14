package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.ChangeCourseSettingResponse;
import edu.ai.lms.core.course.app.port.in.command.ChangeCourseSettingCommand;

public interface TeacherChangeCourseSettingUseCase {
    ChangeCourseSettingResponse changeCourseSetting(ChangeCourseSettingCommand command);
}
