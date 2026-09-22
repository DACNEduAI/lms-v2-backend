package edu.ai.lms.core.quiz3.app.port.out;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ManageQuizReleasePort {
    UUID publishRelease(UUID quizId, long quizVersion, UUID publishedBy, LocalDateTime publishedAt);

    Optional<UUID> findActiveReleaseId(UUID quizId);

    void deactivate(UUID quizId);
}
