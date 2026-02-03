package edu.ai.lms.core.auth.app.port.in.usecase;

import edu.ai.lms.core.auth.adapter.in.request.StudentRegisterRequest;
import edu.ai.lms.core.auth.adapter.in.response.StudentRegisterResponse;


public interface StudentRegisterUseCase {
    StudentRegisterResponse studentRegister(StudentRegisterRequest request);
}
