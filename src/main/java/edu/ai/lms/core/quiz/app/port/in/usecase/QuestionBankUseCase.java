package edu.ai.lms.core.quiz.app.port.in.usecase;

import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelOption;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankQuestion;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankPage;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSearchCriteria;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionDraft;
import edu.ai.lms.core.quiz.app.port.in.command.QuestionBankItemCommand;

public interface QuestionBankUseCase {
    QuestionDraft createDraft(UUID courseId, QuestionBankItemCommand command);

    QuestionBankPage<QuestionDraft> findDraftPage(QuestionBankSearchCriteria criteria);

    QuestionBankPage<QuestionBankQuestion> findBankPage(QuestionBankSearchCriteria criteria);

    QuestionDraft getDraft(UUID courseId, UUID draftId);

    QuestionBankQuestion getBankQuestion(UUID courseId, UUID questionId);

    QuestionDraft updateDraft(UUID courseId, UUID draftId, long version, QuestionBankItemCommand command);

    QuestionBankQuestion validateDraft(UUID courseId, UUID draftId, long version);

    void deleteDraft(UUID courseId, UUID draftId, long version);

    List<CognitiveLevelOption> getCognitiveLevels(UUID courseId);

}
