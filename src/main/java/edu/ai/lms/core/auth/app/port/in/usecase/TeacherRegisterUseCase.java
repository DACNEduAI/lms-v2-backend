package edu.ai.lms.core.auth.app.port.in.usecase;

import edu.ai.lms.core.auth.adapter.in.request.TeacherRegisterRequest;
import edu.ai.lms.core.auth.adapter.in.response.TeacherRegisterResponse;

public interface TeacherRegisterUseCase {
    TeacherRegisterResponse teacherRegister(TeacherRegisterRequest request);
}
