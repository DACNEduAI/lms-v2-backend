package edu.ai.lms.core.quiz.app.port.out;

import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankItem;

public interface SaveQuestionBankItemPort {
    QuestionBankItem create(QuestionBankItem item);

    boolean replaceDraft(QuestionBankItem item, long expectedVersion);

    boolean markReady(QuestionBankItem item, long expectedVersion);

    boolean archive(QuestionBankItem item, long expectedVersion);

    boolean deleteDraft(QuestionBankItem item, long expectedVersion);
}
