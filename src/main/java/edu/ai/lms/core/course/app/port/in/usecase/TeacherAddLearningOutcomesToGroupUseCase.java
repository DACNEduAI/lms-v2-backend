package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.AddLearningOutcomesToGroupResponse;
import edu.ai.lms.core.course.app.port.in.command.AddLearningOutcomesToGroupCommand;

public interface TeacherAddLearningOutcomesToGroupUseCase {
    AddLearningOutcomesToGroupResponse addLearningOutcomesToGroup(AddLearningOutcomesToGroupCommand command);
}
