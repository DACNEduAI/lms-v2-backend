package edu.ai.lms.core.quiz.app.domain.questionbank;

import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.Question;

public record QuestionBankSearchCriteria(
        UUID courseId,
        String query,
        QuestionBankItemStatus status,
        UUID chapterId,
        UUID ccctItemId,
        UUID learningOutcomeId,
        Question.QuestionType questionType,
        QuestionBankSourceType sourceType,
        int page,
        int size) {
    public int offset() {
        return page * size;
    }
}
