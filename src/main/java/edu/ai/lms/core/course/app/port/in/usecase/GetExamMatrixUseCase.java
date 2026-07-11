package edu.ai.lms.core.course.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.course.adapter.in.response.ExamMatrixResponse;

public interface GetExamMatrixUseCase {
    ExamMatrixResponse getExamMatrix(UUID courseId, UUID matrixId);
}
