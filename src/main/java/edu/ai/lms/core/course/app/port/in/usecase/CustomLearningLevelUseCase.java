package edu.ai.lms.core.course.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.course.adapter.in.response.*;
import edu.ai.lms.core.course.app.port.in.command.BatchEditCustomLearningLevelsCommand;
import edu.ai.lms.core.course.app.port.in.command.CustomLearningLevelValue;

public interface CustomLearningLevelUseCase {
    CustomLearningLevelResponse create(UUID courseId, UUID matrixId, CustomLearningLevelValue value);
    CustomLearningLevelListResponse getAll(UUID courseId, UUID matrixId);
    CustomLearningLevelResponse getOne(UUID courseId, UUID matrixId, UUID levelId);
    BatchEditCustomLearningLevelsResponse batchEdit(BatchEditCustomLearningLevelsCommand command);
    DeleteCustomLearningLevelResponse delete(UUID courseId, UUID matrixId, UUID levelId);
}
