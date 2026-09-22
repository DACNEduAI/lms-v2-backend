package edu.ai.lms.core.quiz3.api;

import java.util.List;
import java.util.UUID;

public record QuestionAnswerKeyView(
        UUID questionId,
        long ktQuestionId,
        String questionType,
        String questionContent,
        String questionContentFormat,
        List<AnswerView> answers,
        List<LearningOutcomeModelView> learningOutcomes
) {
    public QuestionAnswerKeyView {
        answers = answers == null ? List.of() : List.copyOf(answers);
        learningOutcomes = learningOutcomes == null ? List.of() : List.copyOf(learningOutcomes);
    }

    public record AnswerView(long answerNumber, String content, String contentFormat, boolean correct) {
    }
}
