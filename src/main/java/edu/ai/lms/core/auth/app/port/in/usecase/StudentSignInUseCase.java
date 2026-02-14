package edu.ai.lms.core.auth.app.port.in.usecase;

import edu.ai.lms.core.auth.adapter.in.response.StudentSignInResponse;
import edu.ai.lms.core.auth.app.port.in.command.StudentSignInCommand;

public interface StudentSignInUseCase {
    StudentSignInResponse signIn(StudentSignInCommand command);
}
