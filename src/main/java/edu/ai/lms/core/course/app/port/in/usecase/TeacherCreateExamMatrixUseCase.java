package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.ExamMatrixResponse;
import edu.ai.lms.core.course.app.port.in.command.CreateExamMatrixCommand;

public interface TeacherCreateExamMatrixUseCase {
    ExamMatrixResponse createExamMatrix(CreateExamMatrixCommand command);
}
