package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.RemoveLearningOutcomesFromGroupResponse;
import edu.ai.lms.core.course.app.port.in.command.RemoveLearningOutcomesFromGroupCommand;

public interface TeacherRemoveLearningOutcomesFromGroupUseCase {
    RemoveLearningOutcomesFromGroupResponse removeLearningOutcomesFromGroup(RemoveLearningOutcomesFromGroupCommand command);
}
