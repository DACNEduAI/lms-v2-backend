package edu.ai.lms.core.quiz3.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.quiz3.adapter.in.http.response.DeleteQuizResponse;

public interface DeleteQuizUseCase {
    DeleteQuizResponse deleteQuiz(UUID courseId, UUID quizId);
}
