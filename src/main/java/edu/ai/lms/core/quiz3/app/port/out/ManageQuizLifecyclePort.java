package edu.ai.lms.core.quiz3.app.port.out;

import java.time.LocalDateTime;
import java.util.UUID;

import edu.ai.lms.core.quiz3.app.domain.QuizLifecycleStatus;

public interface ManageQuizLifecyclePort {
    boolean touchContent(UUID courseId, UUID quizId, long expectedVersion, QuizLifecycleStatus expectedStatus);

    boolean transition(
            UUID courseId,
            UUID quizId,
            long expectedVersion,
            QuizLifecycleStatus expectedStatus,
            QuizLifecycleStatus nextStatus,
            boolean display,
            UUID publishedBy,
            LocalDateTime publishedAt);
}
