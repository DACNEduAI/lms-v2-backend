package edu.ai.lms.core.quiz.app.port.in.usecase;

import edu.ai.lms.core.quiz.adapter.in.http.response.GetAllQuizzesResponse;

import java.util.UUID;

public interface GetAllQuizzesUseCase {
    GetAllQuizzesResponse getAllQuizzes(UUID courseId, int pageSize, String sortDirection);
}
