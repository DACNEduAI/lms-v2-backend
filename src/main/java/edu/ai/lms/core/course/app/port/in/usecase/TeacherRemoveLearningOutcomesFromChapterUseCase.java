package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.RemoveLearningOutcomesFromChapterResponse;
import edu.ai.lms.core.course.app.port.in.command.RemoveLearningOutcomesFromChapterCommand;

public interface TeacherRemoveLearningOutcomesFromChapterUseCase {
    RemoveLearningOutcomesFromChapterResponse removeLearningOutcomesFromChapter(RemoveLearningOutcomesFromChapterCommand command);
}
