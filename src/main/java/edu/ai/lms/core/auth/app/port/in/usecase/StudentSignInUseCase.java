package edu.ai.lms.core.auth.app.port.in.usecase;

import edu.ai.lms.core.auth.adapter.in.request.StudentSignInRequest;
import edu.ai.lms.core.auth.adapter.in.response.StudentSignInResponse;

public interface StudentSignInUseCase {
    StudentSignInResponse signIn(StudentSignInRequest request);
}
