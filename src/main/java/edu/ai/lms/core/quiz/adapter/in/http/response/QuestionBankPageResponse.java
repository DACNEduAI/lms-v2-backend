package edu.ai.lms.core.quiz.adapter.in.http.response;

import java.util.List;

import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankPage;

public record QuestionBankPageResponse(
        List<QuestionBankItemResponse> items,
        long total,
        int page,
        int size) {
    public static QuestionBankPageResponse fromDrafts(QuestionBankPage<edu.ai.lms.core.quiz.app.domain.questionbank.QuestionDraft> page) {
        return new QuestionBankPageResponse(
                page.items().stream().map(QuestionBankItemResponse::from).toList(),
                page.total(), page.page(), page.size());
    }

    public static QuestionBankPageResponse fromBankQuestions(QuestionBankPage<edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankQuestion> page) {
        return new QuestionBankPageResponse(
                page.items().stream().map(QuestionBankItemResponse::from).toList(),
                page.total(), page.page(), page.size());
    }
}
