package edu.ai.lms.core.quiz3.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.quiz3.adapter.in.http.response.GetQuizResponse;

public interface GetQuizUseCase {
    GetQuizResponse getQuiz(UUID courseId, UUID quizId);
}
