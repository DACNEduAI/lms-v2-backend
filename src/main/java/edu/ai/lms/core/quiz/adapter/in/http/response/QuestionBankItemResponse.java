package edu.ai.lms.core.quiz.adapter.in.http.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankAnswer;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankItem;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankItemStatus;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSourceType;

public record QuestionBankItemResponse(
        UUID id,
        UUID courseId,
        UUID chapterId,
        UUID ccctItemId,
        String content,
        Question.QuestionType questionType,
        String cognitiveLevelCode,
        String difficulty,
        QuestionBankItemStatus status,
        QuestionBankSourceType sourceType,
        UUID createdBy,
        UUID readyBy,
        LocalDateTime readyAt,
        long version,
        List<AnswerResponse> answers,
        Set<UUID> learningOutcomeIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean duplicateWarning) {

    public static QuestionBankItemResponse from(QuestionBankItem item) {
        return from(item, false);
    }

    public static QuestionBankItemResponse from(QuestionBankItem item, boolean duplicateWarning) {
        return new QuestionBankItemResponse(
                item.getId(), item.getCourseId(), item.getChapterId(), item.getCcctItemId(), item.getContent(),
                item.getQuestionType(), item.getCognitiveLevelCode(), item.getDifficulty(), item.getStatus(),
                item.getSourceType(), item.getCreatedBy(), item.getReadyBy(), item.getReadyAt(), item.getVersion(),
                item.getAnswers() == null ? List.of() : item.getAnswers().stream().map(AnswerResponse::from).toList(),
                item.getLearningOutcomeIds() == null ? Set.of() : Set.copyOf(item.getLearningOutcomeIds()),
                item.getCreatedAt(), item.getUpdatedAt(), duplicateWarning);
    }

    public record AnswerResponse(
            UUID id,
            String content,
            boolean correct,
            String explanation,
            int orderIndex) {
        static AnswerResponse from(QuestionBankAnswer answer) {
            return new AnswerResponse(answer.getId(), answer.getContent(), answer.isCorrect(),
                    answer.getExplanation(), answer.getOrderIndex());
        }
    }
}
