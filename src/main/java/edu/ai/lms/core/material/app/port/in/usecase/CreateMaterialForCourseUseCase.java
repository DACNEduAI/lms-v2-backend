package edu.ai.lms.core.material.app.port.in.usecase;

import edu.ai.lms.core.material.adapter.in.response.CreateMaterialResponse;
import edu.ai.lms.core.material.app.port.in.command.CreateMaterialForCourseCommand;

public interface CreateMaterialForCourseUseCase {
    CreateMaterialResponse createMaterialForCourse(CreateMaterialForCourseCommand command);
}
