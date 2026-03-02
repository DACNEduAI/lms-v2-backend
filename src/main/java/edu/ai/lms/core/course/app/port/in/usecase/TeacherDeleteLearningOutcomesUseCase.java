package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.DeleteLearningOutcomesResponse;
import edu.ai.lms.core.course.app.port.in.command.DeleteLearningOutcomesCommand;

public interface TeacherDeleteLearningOutcomesUseCase {
    DeleteLearningOutcomesResponse deleteLearningOutcomes(DeleteLearningOutcomesCommand command);
}
