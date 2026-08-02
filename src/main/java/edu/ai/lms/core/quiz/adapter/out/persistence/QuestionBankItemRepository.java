package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankAnswer;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankItem;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSearchCriteria;

@Mapper
interface QuestionBankItemRepository {
    Optional<QuestionBankItem> findByCourseIdAndId(
            @Param("courseId") UUID courseId,
            @Param("itemId") UUID itemId);

    List<QuestionBankAnswer> findAnswersByItemId(@Param("itemId") UUID itemId);

    List<UUID> findLearningOutcomeIdsByItemId(@Param("itemId") UUID itemId);

    List<QuestionBankItem> findPage(QuestionBankSearchCriteria criteria);

    long countPage(QuestionBankSearchCriteria criteria);

    int existsExactDuplicate(
            @Param("courseId") UUID courseId,
            @Param("content") String content,
            @Param("excludedItemId") UUID excludedItemId,
            @Param("readyOnly") boolean readyOnly);

    int insertItem(QuestionBankItem item);

    int updateDraft(
            @Param("item") QuestionBankItem item,
            @Param("expectedVersion") long expectedVersion);

    int markReady(
            @Param("item") QuestionBankItem item,
            @Param("expectedVersion") long expectedVersion);

    int archive(
            @Param("item") QuestionBankItem item,
            @Param("expectedVersion") long expectedVersion);

    int deleteDraft(
            @Param("item") QuestionBankItem item,
            @Param("expectedVersion") long expectedVersion);

    int deleteAnswersByItemId(@Param("itemId") UUID itemId);

    int deleteLearningOutcomesByItemId(@Param("itemId") UUID itemId);

    int insertAnswers(@Param("answers") List<QuestionBankAnswer> answers);

    int insertLearningOutcomes(
            @Param("itemId") UUID itemId,
            @Param("learningOutcomeIds") List<UUID> learningOutcomeIds);
}
