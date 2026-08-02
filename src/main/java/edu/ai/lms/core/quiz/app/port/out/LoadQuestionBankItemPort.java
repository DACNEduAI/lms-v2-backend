package edu.ai.lms.core.quiz.app.port.out;

import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankItem;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankPage;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSearchCriteria;

public interface LoadQuestionBankItemPort {
    Optional<QuestionBankItem> findByCourseIdAndId(UUID courseId, UUID itemId);

    QuestionBankPage findPage(QuestionBankSearchCriteria criteria);

    boolean existsExactDuplicate(UUID courseId, String content, UUID excludedItemId, boolean readyOnly);
}
