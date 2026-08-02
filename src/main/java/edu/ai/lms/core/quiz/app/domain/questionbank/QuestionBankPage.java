package edu.ai.lms.core.quiz.app.domain.questionbank;

import java.util.List;

public record QuestionBankPage(List<QuestionBankItem> items, long total, int page, int size) {
}
