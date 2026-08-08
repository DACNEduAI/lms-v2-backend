package edu.ai.lms.core.course.api;

import java.util.UUID;

public interface QuestionBankReferenceApi {
    boolean chapterBelongsToCourse(UUID courseId, UUID chapterId);
}
