package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.ModifyLearningOutcomesResponse;
import edu.ai.lms.core.course.app.port.in.command.ModifyLearningOutcomesCommand;

public interface TeacherModifyLearningOutcomesUseCase {
    ModifyLearningOutcomesResponse modifyLearningOutcomes(ModifyLearningOutcomesCommand command);
}
