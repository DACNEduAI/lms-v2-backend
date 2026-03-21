package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import edu.ai.lms.core.quiz.app.domain.QuizLearningOutcomeView;

@Mapper
interface QuizLearningOutcomeRepository {

    void insertBatch(@Param("quizId") UUID quizId, @Param("learningOutcomeIds") List<UUID> learningOutcomeIds);

    void deleteBatch(@Param("quizId") UUID quizId, @Param("learningOutcomeIds") List<UUID> learningOutcomeIds);

    List<UUID> findLearningOutcomeIdsByQuizId(@Param("quizId") UUID quizId);

        List<UUID> findExistingLearningOutcomeIdsByCourseIdAndIds(
            @Param("courseId") UUID courseId,
            @Param("learningOutcomeIds") List<UUID> learningOutcomeIds);

        List<UUID> findExistingGroupLearningOutcomeIdsByCourseIdAndIds(
            @Param("courseId") UUID courseId,
            @Param("groupLearningOutcomeIds") List<UUID> groupLearningOutcomeIds);

        List<UUID> findLearningOutcomeIdsByCourseIdAndGroupIds(
            @Param("courseId") UUID courseId,
            @Param("groupLearningOutcomeIds") List<UUID> groupLearningOutcomeIds);

        List<QuizLearningOutcomeView> findQuizLearningOutcomes(
            @Param("courseId") UUID courseId,
            @Param("quizId") UUID quizId);
}
