package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;
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
            int inserted = insert(entity);
            if (inserted == 0 && entity.getIdempotencyKey() != null) {
                return findByIdempotencyKey(entity.getRequestedBy(), entity.getIdempotencyKey()).orElseThrow();
            }
        } else {
            int updated = update(entity);
            if (updated == 0) {
                insert(entity);
            }
        }
        return getById(entity.getId()).orElseThrow();
    }

    Optional<QuizGenerationJob> getById(@Param("jobId") UUID jobId);

    Optional<QuizGenerationJob> getByIdForUpdate(@Param("jobId") UUID jobId);

    Optional<QuizGenerationJob> findByIdempotencyKey(
            @Param("requestedBy") UUID requestedBy,
            @Param("idempotencyKey") String idempotencyKey);

    int claim(@Param("jobId") UUID jobId, @Param("leaseUntil") LocalDateTime leaseUntil);

    List<UUID> findRecoverableIds(@Param("limit") int limit);

    int deleteTerminalBefore(@Param("cutoff") LocalDateTime cutoff);
}
