package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuizGenerationJobPersistenceAdapter implements SaveQuizGenerationJobPort, LoadQuizGenerationJobPort {

    private final QuizGenerationJobRepository quizGenerationJobRepository;

    @Override
    public QuizGenerationJob save(QuizGenerationJob job) {
        return quizGenerationJobRepository.save(job);
    }

    @Override
    public Optional<QuizGenerationJob> getById(UUID jobId) {
        return quizGenerationJobRepository.getById(jobId);
    }

    @Override
    public Optional<QuizGenerationJob> getByIdForUpdate(UUID jobId) {
        return quizGenerationJobRepository.getByIdForUpdate(jobId);
    }

    @Override
    public Optional<QuizGenerationJob> findByIdempotencyKey(UUID requestedBy, String idempotencyKey) {
        return quizGenerationJobRepository.findByIdempotencyKey(requestedBy, idempotencyKey);
    }

    @Override
    public List<UUID> findRecoverableIds(int limit) {
        return quizGenerationJobRepository.findRecoverableIds(limit);
    }

    @Override
    public boolean claim(UUID jobId, LocalDateTime leaseUntil) {
        return quizGenerationJobRepository.claim(jobId, leaseUntil) == 1;
    }

    @Override
    public boolean completeVisualPhase(UUID jobId, String errorCode, String errorMessage,
                                       LocalDateTime completedAt) {
        return quizGenerationJobRepository.completeVisualPhase(
                jobId, errorCode, errorMessage, completedAt) == 1;
    }

    @Override
    public int deleteTerminalBefore(LocalDateTime cutoff) {
        return quizGenerationJobRepository.deleteTerminalBefore(cutoff);
    }
}
