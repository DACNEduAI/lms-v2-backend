package edu.ai.lms.core.quiz.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankItem;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankPage;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSearchCriteria;
import edu.ai.lms.core.quiz.app.port.in.command.QuestionBankItemCommand;

public interface QuestionBankUseCase {
    MutationResult createDraft(UUID courseId, QuestionBankItemCommand command);

    QuestionBankPage findPage(QuestionBankSearchCriteria criteria);

    QuestionBankItem get(UUID courseId, UUID itemId);

    MutationResult updateDraft(UUID courseId, UUID itemId, long version, QuestionBankItemCommand command);

    QuestionBankItem markReady(UUID courseId, UUID itemId, long version);

    QuestionBankItem archive(UUID courseId, UUID itemId, long version);

    MutationResult duplicate(UUID courseId, UUID itemId, long version);

    void deleteDraft(UUID courseId, UUID itemId, long version);

    record MutationResult(QuestionBankItem item, boolean duplicateWarning) {
    }
}
