package edu.ai.lms.core.material.app.port.in.usecase;

import edu.ai.lms.core.material.adapter.in.response.CreateMaterialResponse;
import edu.ai.lms.core.material.app.port.in.command.CreateMaterialForChapterCommand;

public interface CreateMaterialForChapterUseCase {
    CreateMaterialResponse createMaterialForChapter(CreateMaterialForChapterCommand command);
}
