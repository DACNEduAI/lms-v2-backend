package edu.ai.lms.core.quiz.app.port.out;

import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import java.time.LocalDateTime;
import java.util.UUID;

public interface SaveQuizGenerationJobPort {
    QuizGenerationJob save(QuizGenerationJob job);

    boolean claim(UUID jobId, LocalDateTime leaseUntil);

    boolean completeVisualPhase(UUID jobId, String errorCode, String errorMessage, LocalDateTime completedAt);

    int deleteTerminalBefore(LocalDateTime cutoff);
}
