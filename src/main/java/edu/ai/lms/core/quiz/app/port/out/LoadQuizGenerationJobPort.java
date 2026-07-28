package edu.ai.lms.core.quiz.app.port.out;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;

public interface LoadQuizGenerationJobPort {
    Optional<QuizGenerationJob> getById(UUID jobId);

    Optional<QuizGenerationJob> getByIdForUpdate(UUID jobId);

    Optional<QuizGenerationJob> findByIdempotencyKey(UUID requestedBy, String idempotencyKey);

    List<UUID> findRecoverableIds(int limit);
}
