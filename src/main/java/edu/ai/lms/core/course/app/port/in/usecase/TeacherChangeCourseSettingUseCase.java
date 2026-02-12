package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.request.ChangeCourseSettingRequest;
import edu.ai.lms.core.course.adapter.in.response.ChangeCourseSettingResponse;

public interface TeacherChangeCourseSettingUseCase {
    ChangeCourseSettingResponse changeCourseSetting(ChangeCourseSettingRequest request);
}
