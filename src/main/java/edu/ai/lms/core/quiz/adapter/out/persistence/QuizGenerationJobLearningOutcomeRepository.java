package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface QuizGenerationJobLearningOutcomeRepository {
    void insertBatch(@Param("jobId") UUID jobId, @Param("learningOutcomeIds") List<UUID> learningOutcomeIds);

    List<UUID> findLearningOutcomeIdsByJobId(@Param("jobId") UUID jobId);
}
