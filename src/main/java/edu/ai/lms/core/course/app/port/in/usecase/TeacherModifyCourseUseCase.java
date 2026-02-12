package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.request.ModifyCourseRequest;
import edu.ai.lms.core.course.adapter.in.response.ModifyCourseResponse;

public interface TeacherModifyCourseUseCase {
    ModifyCourseResponse modifyCourse(ModifyCourseRequest request);
}
