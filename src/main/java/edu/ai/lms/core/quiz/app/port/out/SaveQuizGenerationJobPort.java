package edu.ai.lms.core.quiz.app.port.out;

import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import java.time.Instant;
import java.util.UUID;

public interface SaveQuizGenerationJobPort {
    QuizGenerationJob save(QuizGenerationJob job);

    boolean claim(UUID jobId, Instant leaseUntil);

    boolean completeVisualPhase(UUID jobId, String errorCode, String errorMessage, Instant completedAt);

    int deleteTerminalBefore(Instant cutoff);
}
