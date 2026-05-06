package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;

@Mapper
interface QuizGenerationJobRepository {

    int insert(QuizGenerationJob entity);

    int update(QuizGenerationJob entity);

    default QuizGenerationJob save(QuizGenerationJob entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
            insert(entity);
        } else {
            int updated = update(entity);
            if (updated == 0) {
                insert(entity);
            }
        }
        return getById(entity.getId()).orElseThrow();
    }

    Optional<QuizGenerationJob> getById(@Param("jobId") UUID jobId);
}
