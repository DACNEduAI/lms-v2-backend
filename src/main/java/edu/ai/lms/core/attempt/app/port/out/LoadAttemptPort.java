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
}
