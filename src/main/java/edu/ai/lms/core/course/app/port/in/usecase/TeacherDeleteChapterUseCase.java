package edu.ai.lms.core.course.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.course.adapter.in.response.DeleteChapterResponse;

public interface TeacherDeleteChapterUseCase {
    DeleteChapterResponse deleteChapter(UUID courseId, UUID chapterId);
}
