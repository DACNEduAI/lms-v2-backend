package edu.ai.lms.core.quiz.api;

import java.util.List;
import java.util.UUID;

public record QuestionAnswerKeyView(
        UUID questionId,
        long ktQuestionId,
        String questionType,
        Long correctAnswerNumber,
        List<LearningOutcomeModelView> learningOutcomes
) {
    public QuestionAnswerKeyView {
        learningOutcomes = learningOutcomes == null ? List.of() : List.copyOf(learningOutcomes);
    }
}
