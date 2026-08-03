package edu.ai.lms.core.quiz.app.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelCode;
import edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelOption;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankQuestion;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankPage;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSearchCriteria;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionDraft;

public interface LoadQuestionBankItemPort {
    Optional<QuestionDraft> findDraftByCourseIdAndId(UUID courseId, UUID draftId);

    Optional<QuestionDraft> findDraftByCourseIdAndIdForUpdate(UUID courseId, UUID draftId);

    Optional<QuestionBankQuestion> findBankByCourseIdAndId(UUID courseId, UUID questionId);

    QuestionBankPage<QuestionDraft> findDraftPage(QuestionBankSearchCriteria criteria);

    QuestionBankPage<QuestionBankQuestion> findBankPage(QuestionBankSearchCriteria criteria);

    boolean existsExactDraftDuplicate(UUID courseId, String content, UUID excludedDraftId);

    boolean existsExactBankDuplicate(UUID courseId, String content);

    Optional<CognitiveLevelOption> findCognitiveLevel(CognitiveLevelCode code);

    List<CognitiveLevelOption> findCognitiveLevels();
}
