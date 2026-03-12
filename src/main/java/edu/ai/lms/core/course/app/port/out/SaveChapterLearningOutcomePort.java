package edu.ai.lms.core.course.app.port.out;

import java.util.UUID;

public interface SaveChapterLearningOutcomePort {
    void addLearningOutcomeToChapter(Long chapterNumber, UUID courseId, UUID learningOutcomeId);
}
