package edu.ai.lms.core.quiz3.api;

import java.time.LocalDateTime;
import java.util.UUID;

/** Public module boundary for publishing immutable quiz releases. */
public interface QuizReleaseApi {
    UUID publish(UUID quizId, long quizVersion, UUID publishedBy, LocalDateTime publishedAt);

    void deactivate(UUID quizId);
}
