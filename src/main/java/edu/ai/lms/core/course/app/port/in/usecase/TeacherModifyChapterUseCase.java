package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.ModifyChapterResponse;
import edu.ai.lms.core.course.app.port.in.command.ModifyChapterCommand;

public interface TeacherModifyChapterUseCase {
    ModifyChapterResponse modifyChapter(ModifyChapterCommand command);
}
