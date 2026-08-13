package edu.ai.lms.core.quiz.adapter.in.http.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonRawValue;

import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelCode;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankAnswer;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankQuestion;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSourceType;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionDraft;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;

public record QuestionBankItemResponse(
        UUID id,
        UUID originDraftId,
        UUID courseId,
        UUID chapterId,
        UUID cognitiveLevelId,
        CognitiveLevelCode cognitiveLevelCode,
        String cognitiveLevelName,
        String content,
        RichContentFormat contentFormat,
        String explanation,
        RichContentFormat explanationFormat,
        Question.QuestionType questionType,
        @JsonRawValue String codeRunnerContract,
        QuestionBankSourceType sourceType,
        UUID createdBy,
        UUID validatedBy,
        LocalDateTime validatedAt,
        long version,
        List<AnswerResponse> answers,
        Set<UUID> learningOutcomeIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static QuestionBankItemResponse from(QuestionDraft draft) {
        return new QuestionBankItemResponse(
                draft.getId(), null, draft.getCourseId(), draft.getChapterId(),
                draft.getCognitiveLevelId(), draft.getCognitiveLevelCode(), draft.getCognitiveLevelName(),
                draft.getContent(), draft.getContentFormat(), draft.getExplanation(), draft.getExplanationFormat(),
                draft.getQuestionType(), draft.getCodeRunnerContractJson(), draft.getSourceType(), draft.getCreatedBy(),
                null, null, draft.getVersion(),
                answers(draft.getAnswers()), outcomes(draft.getLearningOutcomeIds()),
                draft.getCreatedAt(), draft.getUpdatedAt());
    }

    public static QuestionBankItemResponse from(QuestionBankQuestion question) {
        return new QuestionBankItemResponse(
                question.getId(), question.getOriginDraftId(), question.getCourseId(), question.getChapterId(),
                question.getCognitiveLevelId(), question.getCognitiveLevelCode(), question.getCognitiveLevelName(),
                question.getContent(), question.getContentFormat(), question.getExplanation(), question.getExplanationFormat(),
                question.getQuestionType(), question.getCodeRunnerContractJson(), question.getSourceType(), question.getCreatedBy(),
                question.getValidatedBy(), question.getValidatedAt(), question.getVersion(),
                answers(question.getAnswers()), outcomes(question.getLearningOutcomeIds()),
                question.getCreatedAt(), question.getUpdatedAt());
    }

    private static List<AnswerResponse> answers(List<QuestionBankAnswer> answers) {
        return answers == null ? List.of() : answers.stream().map(AnswerResponse::from).toList();
    }

    private static Set<UUID> outcomes(Set<UUID> outcomes) {
        return outcomes == null ? Set.of() : Set.copyOf(outcomes);
    }

    public record AnswerResponse(UUID id, String content, RichContentFormat contentFormat,
            boolean correct, int orderIndex) {
        static AnswerResponse from(QuestionBankAnswer answer) {
            return new AnswerResponse(answer.getId(), answer.getContent(), answer.getContentFormat(), answer.isCorrect(),
                    answer.getOrderIndex());
        }
    }
}
