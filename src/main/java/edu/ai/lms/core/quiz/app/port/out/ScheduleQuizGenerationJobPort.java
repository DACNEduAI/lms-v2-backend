package edu.ai.lms.core.quiz.app.port.out;

import java.util.UUID;
import java.time.LocalDateTime;

public interface ScheduleQuizGenerationJobPort {
    void schedule(UUID jobId, LocalDateTime fireAt);

    void unschedule(UUID jobId);
}
