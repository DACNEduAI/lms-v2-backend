package edu.ai.lms.core.quiz.app.port.out;

import java.time.Instant;
import java.util.UUID;

public interface ScheduleQuestionVisualJobPort {
    void scheduleImmediate(UUID parentJobId, Instant fireAt);

    void scheduleRetry(UUID visualJobId, Instant fireAt);

    void scheduleLeaseRecovery(UUID visualJobId, Instant fireAt);

    void scheduleDeadline(UUID parentJobId, Instant fireAt);

    void cancelChild(UUID visualJobId);

    void cancelParent(UUID parentJobId);
}
