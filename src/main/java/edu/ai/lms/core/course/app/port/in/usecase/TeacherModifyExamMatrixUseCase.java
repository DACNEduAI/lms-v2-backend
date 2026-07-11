package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.ModifyExamMatrixResponse;
import edu.ai.lms.core.course.app.port.in.command.ModifyExamMatrixCommand;

public interface TeacherModifyExamMatrixUseCase {
    ModifyExamMatrixResponse modifyExamMatrix(ModifyExamMatrixCommand command);
}
