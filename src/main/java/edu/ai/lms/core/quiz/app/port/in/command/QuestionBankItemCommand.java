package edu.ai.lms.core.quiz.app.port.in.command;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelCode;

public record QuestionBankItemCommand(
        UUID chapterId,
        String content,
        Question.QuestionType questionType,
        CognitiveLevelCode cognitiveLevelCode,
        List<Answer> answers,
        Set<UUID> learningOutcomeIds) {

    public record Answer(String content, boolean correct, String explanation) {
    }
}
