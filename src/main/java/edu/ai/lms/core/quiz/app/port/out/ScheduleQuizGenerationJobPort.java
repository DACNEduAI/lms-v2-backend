package edu.ai.lms.core.quiz.app.port.out;

import java.util.UUID;
import java.time.Instant;

public interface ScheduleQuizGenerationJobPort {
    void schedule(UUID jobId, Instant fireAt);

    void unschedule(UUID jobId);
}
