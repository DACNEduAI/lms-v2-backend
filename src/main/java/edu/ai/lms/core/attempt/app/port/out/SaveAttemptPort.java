package edu.ai.lms.core.attempt.app.port.out;

import edu.ai.lms.core.attempt.app.domain.Attempt;

import java.time.LocalDateTime;
import java.util.UUID;

public interface SaveAttemptPort {

    Attempt save(Attempt attempt);

    boolean closeIfStarted(
            UUID attemptId,
            Attempt.AttemptStatus status,
            Double score,
            String errorReason,
            LocalDateTime submittedTime
    );
}
