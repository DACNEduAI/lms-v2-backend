package edu.ai.lms.core.auth.app.port.in.usecase;

import edu.ai.lms.core.auth.adapter.in.request.StudentRegisterRequest;


public interface StudentRegisterUseCase {
    public boolean studentRegister(StudentRegisterRequest request);
}
