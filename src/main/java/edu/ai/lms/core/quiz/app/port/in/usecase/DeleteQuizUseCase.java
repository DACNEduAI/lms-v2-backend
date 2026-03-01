package edu.ai.lms.core.quiz.app.port.in.usecase;

import edu.ai.lms.core.quiz.adapter.in.http.response.DeleteQuizResponse;

import java.util.UUID;

public interface DeleteQuizUseCase {
    DeleteQuizResponse deleteQuiz(UUID courseId, UUID quizId);
}
