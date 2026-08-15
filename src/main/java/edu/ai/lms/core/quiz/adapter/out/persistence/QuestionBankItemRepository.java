package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelCode;
import edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelOption;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankAnswer;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankQuestion;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSearchCriteria;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionDraft;

@Mapper
interface QuestionBankItemRepository {
    Optional<QuestionDraft> findDraftByCourseIdAndId(
            @Param("courseId") UUID courseId,
            @Param("itemId") UUID itemId);

    Optional<QuestionDraft> findDraftById(@Param("itemId") UUID itemId);

    Optional<QuestionDraft> findDraftByCourseIdAndIdForUpdate(
            @Param("courseId") UUID courseId,
            @Param("itemId") UUID itemId);

    Optional<QuestionBankQuestion> findBankByCourseIdAndId(
            @Param("courseId") UUID courseId,
            @Param("itemId") UUID itemId);

    Optional<QuestionBankQuestion> findBankQuestionById(@Param("questionId") UUID questionId);

    List<QuestionBankAnswer> findDraftAnswers(@Param("itemId") UUID itemId);

    List<QuestionBankAnswer> findBankAnswers(@Param("itemId") UUID itemId);

    List<UUID> findDraftLearningOutcomeIds(@Param("itemId") UUID itemId);

    List<UUID> findBankLearningOutcomeIds(@Param("itemId") UUID itemId);

    List<QuestionDraft> findDraftPage(QuestionBankSearchCriteria criteria);

    long countDraftPage(QuestionBankSearchCriteria criteria);

    List<QuestionBankQuestion> findBankPage(QuestionBankSearchCriteria criteria);

    long countBankPage(QuestionBankSearchCriteria criteria);

    int existsExactDraftDuplicate(
            @Param("courseId") UUID courseId,
            @Param("content") String content,
            @Param("excludedItemId") UUID excludedItemId);

    int existsExactBankDuplicate(
            @Param("courseId") UUID courseId,
            @Param("content") String content);

    Optional<CognitiveLevelOption> findCognitiveLevel(@Param("code") CognitiveLevelCode code);

    List<CognitiveLevelOption> findCognitiveLevels();

    int insertDraft(QuestionDraft draft);

    int updateDraft(
            @Param("item") QuestionDraft draft,
            @Param("expectedVersion") long expectedVersion);

    int insertBankQuestion(QuestionBankQuestion question);

    int deleteDraft(
            @Param("item") QuestionDraft draft,
            @Param("expectedVersion") long expectedVersion);

    int deleteDraftAnswers(@Param("itemId") UUID itemId);

    int deleteDraftLearningOutcomes(@Param("itemId") UUID itemId);

    int insertDraftAnswers(@Param("answers") List<QuestionBankAnswer> answers);

    int insertBankAnswers(@Param("answers") List<QuestionBankAnswer> answers);

    int insertDraftLearningOutcomes(
            @Param("itemId") UUID itemId,
            @Param("learningOutcomeIds") List<UUID> learningOutcomeIds);

    int insertBankLearningOutcomes(
            @Param("itemId") UUID itemId,
            @Param("learningOutcomeIds") List<UUID> learningOutcomeIds);
}
