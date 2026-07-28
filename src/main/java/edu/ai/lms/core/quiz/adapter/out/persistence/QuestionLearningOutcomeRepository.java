package edu.ai.lms.core.quiz.adapter.out.persistence;

import edu.ai.lms.core.quiz.app.domain.QuestionLearningOutcomeLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
interface QuestionLearningOutcomeRepository {

    void insertBatch(@Param("questionId") UUID questionId, @Param("learningOutcomeIds") List<UUID> learningOutcomeIds);

    void deleteBatch(@Param("questionId") UUID questionId, @Param("learningOutcomeIds") List<UUID> learningOutcomeIds);

    List<UUID> findLearningOutcomeIdsByQuestionId(@Param("questionId") UUID questionId);

    List<QuestionLearningOutcomeLink> findLinksByQuizId(@Param("quizId") UUID quizId);
}
