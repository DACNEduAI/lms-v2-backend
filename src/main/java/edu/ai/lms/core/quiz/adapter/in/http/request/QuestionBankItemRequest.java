package edu.ai.lms.core.quiz.adapter.in.http.request;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelCode;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.port.in.command.QuestionBankItemCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuestionBankItemRequest(
        UUID chapterId,
        @NotBlank @Size(max = 262144) String content,
        RichContentFormat contentFormat,
        @Size(max = 131072) String explanation,
        RichContentFormat explanationFormat,
        @NotNull Question.QuestionType questionType,
        CognitiveLevelCode cognitiveLevelCode,
        @Size(max = 10) List<@Valid AnswerRequest> answers,
        Set<UUID> learningOutcomeIds) {

    public QuestionBankItemRequest(UUID chapterId, String content, Question.QuestionType questionType,
            CognitiveLevelCode cognitiveLevelCode, List<AnswerRequest> answers, Set<UUID> learningOutcomeIds) {
        this(chapterId, content, RichContentFormat.PLAIN_TEXT, null, RichContentFormat.PLAIN_TEXT,
                questionType, cognitiveLevelCode, answers, learningOutcomeIds);
    }

    public QuestionBankItemCommand toCommand() {
        return new QuestionBankItemCommand(
                chapterId,
                content,
                contentFormat == null ? RichContentFormat.PLAIN_TEXT : contentFormat,
                explanation,
                explanationFormat == null ? RichContentFormat.PLAIN_TEXT : explanationFormat,
                questionType,
                cognitiveLevelCode,
                answers == null ? List.of() : answers.stream().map(AnswerRequest::toCommand).toList(),
                learningOutcomeIds == null ? Set.of() : learningOutcomeIds);
    }

    public record AnswerRequest(
            @NotBlank @Size(max = 131072) String content,
            RichContentFormat contentFormat,
            boolean correct) {
        public AnswerRequest(String content, boolean correct, String ignoredExplanation) {
            this(content, RichContentFormat.PLAIN_TEXT, correct);
        }

        public AnswerRequest(String content, RichContentFormat contentFormat, boolean correct,
                String ignoredExplanation, RichContentFormat ignoredExplanationFormat) {
            this(content, contentFormat, correct);
        }

        QuestionBankItemCommand.Answer toCommand() {
            return new QuestionBankItemCommand.Answer(content,
                    contentFormat == null ? RichContentFormat.PLAIN_TEXT : contentFormat,
                    correct);
        }
    }
}
