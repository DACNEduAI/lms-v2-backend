package edu.ai.lms.core.course.app.port.in.usecase;

import java.util.List;

import edu.ai.lms.core.course.adapter.in.response.GetGroupLearningOutcomeResponse;
import edu.ai.lms.core.course.app.port.in.command.ReorderLearningOutcomeTreeCommand;

public interface TeacherReorderLearningOutcomeTreeUseCase {
    List<GetGroupLearningOutcomeResponse> reorderLearningOutcomeTree(ReorderLearningOutcomeTreeCommand command);
}
