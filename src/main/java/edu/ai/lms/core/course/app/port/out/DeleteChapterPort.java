package edu.ai.lms.core.course.app.port.out;

import java.util.UUID;

public interface DeleteChapterPort {
    void deleteByKey(UUID courseId, UUID chapterId);
}
