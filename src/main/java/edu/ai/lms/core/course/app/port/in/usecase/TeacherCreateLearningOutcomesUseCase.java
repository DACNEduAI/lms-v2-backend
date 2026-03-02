package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.CreateLearningOutcomesResponse;
import edu.ai.lms.core.course.app.port.in.command.CreateLearningOutcomesCommand;

public interface TeacherCreateLearningOutcomesUseCase {
    CreateLearningOutcomesResponse createLearningOutcomes(CreateLearningOutcomesCommand command);
}
