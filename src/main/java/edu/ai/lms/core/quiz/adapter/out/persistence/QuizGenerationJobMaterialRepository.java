package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QuizGenerationJobMaterialRepository {

    void insert(@Param("jobId") UUID jobId, @Param("materialId") UUID materialId);

    void deleteByJobId(@Param("jobId") UUID jobId);

    List<UUID> findMaterialIdsByJobId(@Param("jobId") UUID jobId);
}
