package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.adapter.in.response.AddLearningOutcomesToChapterResponse;
import edu.ai.lms.core.course.app.port.in.command.AddLearningOutcomesToChapterCommand;

public interface TeacherAddLearningOutcomesToChapterUseCase {
    AddLearningOutcomesToChapterResponse addLearningOutcomesToChapter(AddLearningOutcomesToChapterCommand command);
}
