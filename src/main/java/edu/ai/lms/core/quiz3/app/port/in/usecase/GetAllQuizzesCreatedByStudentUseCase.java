package edu.ai.lms.core.quiz3.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.quiz3.adapter.in.http.response.GetAllQuizzesResponse;

public interface GetAllQuizzesCreatedByStudentUseCase {

    GetAllQuizzesResponse getAllQuizzesCreatedByStudent(UUID courseId, int pageSize, String lastCreated);
}
