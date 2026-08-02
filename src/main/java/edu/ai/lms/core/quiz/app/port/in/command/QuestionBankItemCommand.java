package edu.ai.lms.core.quiz.app.port.in.command;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.Question;

public record QuestionBankItemCommand(
        UUID chapterId,
        UUID ccctItemId,
        String content,
        Question.QuestionType questionType,
        String cognitiveLevelCode,
        String difficulty,
        List<Answer> answers,
        Set<UUID> learningOutcomeIds) {

    public record Answer(String content, boolean correct, String explanation) {
    }
}
