package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import edu.ai.lms.core.quiz.app.domain.QuestionGenerationProvenance;

@Mapper
interface QuestionGenerationProvenanceRepository {
    Optional<QuestionGenerationProvenance> findByQuestionId(@Param("questionId") UUID questionId);

    int insert(QuestionGenerationProvenance provenance);
}
