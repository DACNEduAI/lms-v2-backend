package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.DeleteGroupLearningOutcomeResponse;
import edu.ai.lms.core.course.app.port.in.command.DeleteGroupLearningOutcomeCommand;

public interface TeacherDeleteGroupLearningOutcomeUseCase {
    DeleteGroupLearningOutcomeResponse deleteGroupLearningOutcome(DeleteGroupLearningOutcomeCommand command);
}
