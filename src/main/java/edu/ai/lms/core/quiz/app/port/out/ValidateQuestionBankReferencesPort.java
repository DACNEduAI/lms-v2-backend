package edu.ai.lms.core.quiz.app.port.out;

import java.util.UUID;

public interface ValidateQuestionBankReferencesPort {
    boolean chapterBelongsToCourse(UUID courseId, UUID chapterId);
}
