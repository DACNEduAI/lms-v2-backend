package edu.ai.lms.core.auth.app.port.in.usecase;

import edu.ai.lms.core.auth.adapter.in.response.TeacherSignInResponse;
import edu.ai.lms.core.auth.app.port.in.command.TeacherSignInCommand;

public interface TeacherSignInUseCase {
    TeacherSignInResponse signIn(TeacherSignInCommand command);
}
