package edu.ai.lms.core.quiz.app.port.out;

import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankItem;

public interface SaveQuestionBankItemPort {
    QuestionBankItem create(QuestionBankItem item);

    boolean replaceDraft(QuestionBankItem item, long expectedVersion);
}
