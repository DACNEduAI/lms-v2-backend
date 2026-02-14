package edu.ai.lms.core.auth.app.port.in.usecase;

import edu.ai.lms.core.auth.adapter.in.response.TeacherRegisterResponse;
import edu.ai.lms.core.auth.app.port.in.command.TeacherRegisterCommand;

public interface TeacherRegisterUseCase {
    TeacherRegisterResponse teacherRegister(TeacherRegisterCommand command);
}
