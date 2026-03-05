package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.ModifyLearningOutcomeResponse;
import edu.ai.lms.core.course.app.port.in.command.ModifySingleLearningOutcomeCommand;

public interface TeacherModifySingleLearningOutcomeUseCase {
    ModifyLearningOutcomeResponse modifyLearningOutcome(ModifySingleLearningOutcomeCommand command);
}
