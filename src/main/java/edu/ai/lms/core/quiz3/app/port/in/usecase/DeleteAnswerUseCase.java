package edu.ai.lms.core.quiz3.app.port.in.usecase;

import edu.ai.lms.core.quiz3.adapter.in.http.response.DeleteAnswerResponse;

import java.util.UUID;

public interface DeleteAnswerUseCase {
    DeleteAnswerResponse deleteAnswer(UUID questionId, Long answerNumber);
}
