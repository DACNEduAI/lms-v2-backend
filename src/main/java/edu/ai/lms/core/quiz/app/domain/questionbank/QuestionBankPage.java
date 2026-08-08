package edu.ai.lms.core.quiz.app.domain.questionbank;

import java.util.List;

public record QuestionBankPage<T>(List<T> items, long total, int page, int size) {
}
