package edu.ai.lms.core.quiz.api;

import java.util.UUID;

public record QuestionAnswerKeyView(
        UUID questionId,
        String questionType,
        Long correctAnswerNumber
) {
}
