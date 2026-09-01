package edu.ai.lms.core.attempt.app.port.out;

import java.util.Map;
import java.util.UUID;

/** Persists the Code Runner submissions selected at the instant an attempt is finished. */
public interface AttemptCodeSubmissionSnapshotPort {

    void replace(UUID attemptId, Map<UUID, UUID> submissionIdsByQuestion);

    Map<UUID, UUID> findByAttemptId(UUID attemptId);
}
