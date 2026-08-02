package edu.ai.lms.core.quiz.app.port.out;

import java.util.Optional;
import java.util.UUID;

public interface ValidateQuestionBankReferencesPort {
    boolean chapterBelongsToCourse(UUID courseId, UUID chapterId);

    Optional<UUID> findPublishedExamCcctItemChapterId(UUID courseId, UUID ccctItemId);
}
