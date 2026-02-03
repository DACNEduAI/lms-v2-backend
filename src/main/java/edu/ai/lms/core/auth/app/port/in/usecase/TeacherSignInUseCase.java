package edu.ai.lms.core.auth.app.port.in.usecase;

import edu.ai.lms.core.auth.adapter.in.request.TeacherSignInRequest;
import edu.ai.lms.core.auth.adapter.in.response.TeacherSignInResponse;

public interface TeacherSignInUseCase {
    TeacherSignInResponse signIn(TeacherSignInRequest request);
}
