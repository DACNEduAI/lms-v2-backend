package edu.ai.lms.core.quiz.app.port.in.usecase;

import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuizResponse;

import java.util.UUID;

public interface GetQuizUseCase {
    GetQuizResponse getQuiz(UUID courseId, UUID quizId);
}
