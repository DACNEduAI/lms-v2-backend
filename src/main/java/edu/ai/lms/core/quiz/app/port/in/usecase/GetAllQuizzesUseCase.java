package edu.ai.lms.core.quiz.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.quiz.adapter.in.http.response.GetAllQuizzesResponse;

public interface GetAllQuizzesUseCase {
    GetAllQuizzesResponse getAllQuizzes(UUID courseId, int pageSize, String sortDirection);
}
