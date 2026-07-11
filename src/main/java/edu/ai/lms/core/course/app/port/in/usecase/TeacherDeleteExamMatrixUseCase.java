package edu.ai.lms.core.course.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.course.adapter.in.response.DeleteExamMatrixResponse;

public interface TeacherDeleteExamMatrixUseCase {
    DeleteExamMatrixResponse deleteExamMatrix(UUID courseId, UUID matrixId);
}
