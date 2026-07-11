package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.GetGroupLearningOutcomeResponse;
import edu.ai.lms.core.course.app.port.in.command.ModifyGroupLearningOutcomeCommand;

public interface TeacherModifyGroupLearningOutcomeUseCase {
    GetGroupLearningOutcomeResponse modifyGroupLearningOutcome(ModifyGroupLearningOutcomeCommand command);
}
