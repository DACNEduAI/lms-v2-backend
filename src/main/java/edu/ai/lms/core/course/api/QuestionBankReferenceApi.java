package edu.ai.lms.core.course.api;

import java.util.Optional;
import java.util.UUID;

public interface QuestionBankReferenceApi {
    boolean chapterBelongsToCourse(UUID courseId, UUID chapterId);

    Optional<UUID> findPublishedExamCcctItemChapterId(UUID courseId, UUID ccctItemId);
}
