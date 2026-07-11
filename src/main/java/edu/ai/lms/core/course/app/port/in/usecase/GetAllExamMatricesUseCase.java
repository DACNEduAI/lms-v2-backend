package edu.ai.lms.core.course.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.course.adapter.in.response.ExamMatrixListResponse;

public interface GetAllExamMatricesUseCase {
    ExamMatrixListResponse getAllExamMatrices(UUID courseId);
}
