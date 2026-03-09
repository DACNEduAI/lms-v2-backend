package edu.ai.lms.core.material.app.port.in.usecase;

import edu.ai.lms.core.material.adapter.in.response.CreateMaterialResponse;
import edu.ai.lms.core.material.app.port.in.command.CreateMaterialForChapterCommand;
import edu.ai.lms.core.material.app.port.in.command.CreateMaterialForCourseCommand;

public interface CreateMaterialUseCase {
    CreateMaterialResponse createMaterialForChapter(CreateMaterialForChapterCommand command);
    CreateMaterialResponse createMaterialForCourse(CreateMaterialForCourseCommand command);
}
