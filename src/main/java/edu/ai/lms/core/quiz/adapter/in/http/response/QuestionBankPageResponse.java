package edu.ai.lms.core.quiz.adapter.in.http.response;

import java.util.List;

import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankPage;

public record QuestionBankPageResponse(
        List<QuestionBankItemResponse> items,
        long total,
        int page,
        int size) {
    public static QuestionBankPageResponse from(QuestionBankPage page) {
        return new QuestionBankPageResponse(
                page.items().stream().map(QuestionBankItemResponse::from).toList(),
                page.total(), page.page(), page.size());
    }
}
