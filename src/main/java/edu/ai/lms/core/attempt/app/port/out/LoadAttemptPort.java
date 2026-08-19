package edu.ai.lms.core.attempt.app.port.out;

import edu.ai.lms.core.attempt.app.domain.Attempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadAttemptPort {

    Optional<Attempt> getById(UUID attemptId);

    long countFinishedAttempts(UUID studentId, UUID quizId);

    Optional<Attempt> getLatestStartedAttempt(UUID studentId, UUID quizId);

    Optional<Attempt> getByIdAndStudentId(UUID attemptId, UUID studentId);

    List<Attempt> getByStudentAndQuiz(UUID studentId, UUID quizId);

    /**
     * Bounded recovery scan for attempts whose selected Code Runner submission
     * completed while the in-process completion event was lost (for example,
     * during a process restart).
     */
    List<Attempt> getFinalizingAttempts();
}
