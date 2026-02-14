package edu.ai.lms.core.auth.app.port.in.usecase;

import edu.ai.lms.core.auth.adapter.in.response.StudentRegisterResponse;
import edu.ai.lms.core.auth.app.port.in.command.StudentRegisterCommand;


public interface StudentRegisterUseCase {
    StudentRegisterResponse studentRegister(StudentRegisterCommand command);
}
