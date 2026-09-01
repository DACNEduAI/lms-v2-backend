package edu.ai.lms.core.attempt.adapter.out.persistence;

import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.SaveAttemptPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AttemptPersistenceAdapter implements SaveAttemptPort, LoadAttemptPort {

    private final AttemptRepository attemptRepository;

    @Override
    public Attempt save(Attempt attempt) {
        return attemptRepository.save(attempt);
    }

    @Override
    public boolean finalizeIfStillFinalizing(UUID attemptId, Double score) {
        return attemptRepository.finalizeIfStillFinalizing(attemptId, score) > 0;
    }

    @Override
    public boolean closeIfStarted(
            UUID attemptId,
            Attempt.AttemptStatus status,
            Double score,
            String errorReason,
            LocalDateTime submittedTime
    ) {
        return attemptRepository.closeIfStarted(attemptId, status, score, errorReason, submittedTime) > 0;
    }

    @Override
    public Optional<Attempt> getById(UUID attemptId) {
        return attemptRepository.getById(attemptId);
    }

    @Override
    public long countFinishedAttempts(UUID studentId, UUID quizId) {
        return attemptRepository.countFinishedAttempts(studentId, quizId);
    }

    @Override
    public Optional<Attempt> getLatestStartedAttempt(UUID studentId, UUID quizId) {
        return attemptRepository.getLatestStartedAttempt(studentId, quizId);
    }

    @Override
    public Optional<Attempt> getLatestActiveAttempt(UUID studentId, UUID quizId) {
        return attemptRepository.getLatestActiveAttempt(studentId, quizId);
    }

    @Override
    public Optional<Attempt> getByIdAndStudentId(UUID attemptId, UUID studentId) {
        return attemptRepository.getByIdAndStudentId(attemptId, studentId);
    }

    @Override
    public List<Attempt> getByStudentAndQuiz(UUID studentId, UUID quizId) {
        return attemptRepository.getByStudentAndQuiz(studentId, quizId);
    }

    @Override
    public List<Attempt> getFinalizingAttempts() {
        return attemptRepository.getFinalizingAttempts();
    }

}
