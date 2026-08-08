package edu.ai.lms.core.quiz.app.domain.questionbank;

import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.Question;

public record QuestionBankSearchCriteria(
        UUID courseId,
        String query,
        UUID chapterId,
        UUID learningOutcomeId,
        Question.QuestionType questionType,
        CognitiveLevelCode cognitiveLevelCode,
        QuestionBankSourceType sourceType,
        int page,
        int size) {
    public int offset() {
        return page * size;
    }
}
