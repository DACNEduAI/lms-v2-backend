package edu.ai.lms.core.quiz.adapter.in.http.request;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.port.in.command.QuestionBankItemCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuestionBankItemRequest(
        UUID chapterId,
        UUID ccctItemId,
        @NotBlank @Size(max = 10000) String content,
        @NotNull Question.QuestionType questionType,
        @Size(max = 100) String cognitiveLevelCode,
        @Size(max = 30) String difficulty,
        @Size(max = 10) List<@Valid AnswerRequest> answers,
        Set<UUID> learningOutcomeIds) {

    public QuestionBankItemCommand toCommand() {
        return new QuestionBankItemCommand(
                chapterId,
                ccctItemId,
                content,
                questionType,
                cognitiveLevelCode,
                difficulty,
                answers == null ? List.of() : answers.stream().map(AnswerRequest::toCommand).toList(),
                learningOutcomeIds == null ? Set.of() : learningOutcomeIds);
    }

    public record AnswerRequest(
            @NotBlank @Size(max = 5000) String content,
            boolean correct,
            @Size(max = 5000) String explanation) {
        QuestionBankItemCommand.Answer toCommand() {
            return new QuestionBankItemCommand.Answer(content, correct, explanation);
        }
    }
}
