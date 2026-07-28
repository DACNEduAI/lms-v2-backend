package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.BatchEditExamMatrixResponse;
import edu.ai.lms.core.course.app.port.in.command.BatchEditExamMatrixCommand;

public interface TeacherBatchEditExamMatrixUseCase {
    BatchEditExamMatrixResponse batchEdit(BatchEditExamMatrixCommand command);
}
