package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.CreateGroupLearningOutcomeResponse;
import edu.ai.lms.core.course.app.port.in.command.CreateGroupLearningOutcomeCommand;

public interface TeacherCreateGroupLearningOutcomeUseCase {
    CreateGroupLearningOutcomeResponse createGroupLearningOutcome(CreateGroupLearningOutcomeCommand command);
}
