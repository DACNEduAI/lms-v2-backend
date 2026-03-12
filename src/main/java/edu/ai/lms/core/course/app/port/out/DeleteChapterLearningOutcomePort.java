package edu.ai.lms.core.course.app.port.out;

import java.util.UUID;

public interface DeleteChapterLearningOutcomePort {
    void removeLearningOutcomeFromChapter(Long chapterNumber, UUID courseId, UUID learningOutcomeId);
}
