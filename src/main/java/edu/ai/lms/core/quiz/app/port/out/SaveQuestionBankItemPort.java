package edu.ai.lms.core.quiz.app.port.out;

import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankQuestion;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionDraft;

public interface SaveQuestionBankItemPort {
    QuestionDraft createDraft(QuestionDraft draft);

    boolean replaceDraft(QuestionDraft draft, long expectedVersion);

    QuestionBankQuestion createBankQuestion(QuestionBankQuestion question);

    boolean deleteDraft(QuestionDraft draft, long expectedVersion);
}
