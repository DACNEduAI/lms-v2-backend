package edu.ai.lms.core.quiz.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationStatusResponse;

public interface GetQuizGenerationJobUseCase {
    QuizGenerationStatusResponse getStatus(UUID jobId);
}
