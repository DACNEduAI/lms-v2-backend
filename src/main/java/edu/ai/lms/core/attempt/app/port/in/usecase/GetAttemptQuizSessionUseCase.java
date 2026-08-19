package edu.ai.lms.core.attempt.app.port.in.usecase;

import edu.ai.lms.core.attempt.adapter.in.response.GetAttemptQuizSessionResponse;

import java.util.UUID;

public interface GetAttemptQuizSessionUseCase {

    GetAttemptQuizSessionResponse getSession(UUID courseId, UUID quizId);
}
