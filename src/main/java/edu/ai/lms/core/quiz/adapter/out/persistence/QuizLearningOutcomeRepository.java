package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


@Mapper
interface QuizLearningOutcomeRepository {

    void insertBatch(@Param("quizId") UUID quizId, @Param("learningOutcomeIds") List<UUID> learningOutcomeIds);

    void deleteBatch(@Param("quizId") UUID quizId, @Param("learningOutcomeIds") List<UUID> learningOutcomeIds);

    List<UUID> findLearningOutcomeIdsByQuizId(@Param("quizId") UUID quizId);

}
