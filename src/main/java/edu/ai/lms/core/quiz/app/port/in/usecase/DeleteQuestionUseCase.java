package edu.ai.lms.core.quiz.app.port.in.usecase;

import edu.ai.lms.core.quiz.adapter.in.http.response.DeleteQuestionResponse;

import java.util.UUID;

public interface DeleteQuestionUseCase {
    DeleteQuestionResponse deleteQuestion(UUID quizId, UUID questionId);
}
