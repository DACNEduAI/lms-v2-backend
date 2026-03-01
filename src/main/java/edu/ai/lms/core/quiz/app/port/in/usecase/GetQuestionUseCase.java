package edu.ai.lms.core.quiz.app.port.in.usecase;

import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuestionResponse;

import java.util.UUID;

public interface GetQuestionUseCase {
    GetQuestionResponse getQuestion(UUID quizId, UUID questionId);
}
