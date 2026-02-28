package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.CreateChapterResponse;
import edu.ai.lms.core.course.app.port.in.command.CreateChapterCommand;

public interface TeacherCreateChapterUseCase {
    CreateChapterResponse createChapter(CreateChapterCommand command);
}
