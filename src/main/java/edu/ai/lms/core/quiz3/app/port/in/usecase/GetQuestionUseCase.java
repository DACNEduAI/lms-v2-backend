package edu.ai.lms.core.quiz3.app.port.in.usecase;

import edu.ai.lms.core.quiz3.adapter.in.http.response.GetQuestionResponse;

import java.util.UUID;

public interface GetQuestionUseCase {
    GetQuestionResponse getQuestion(UUID quizId, UUID questionId);
}
