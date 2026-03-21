package edu.ai.lms.core.attempt.app.port.in.usecase;

import edu.ai.lms.core.attempt.adapter.in.response.GetMyAttemptsResponse;

import java.util.UUID;

public interface GetMyAttemptsUseCase {

    GetMyAttemptsResponse getMyAttempts(UUID courseId, UUID quizId);
}
