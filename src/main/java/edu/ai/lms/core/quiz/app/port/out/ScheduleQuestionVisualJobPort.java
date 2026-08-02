package edu.ai.lms.core.quiz.app.port.out;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ScheduleQuestionVisualJobPort {
    void scheduleImmediate(UUID parentJobId, LocalDateTime fireAt);

    void scheduleRetry(UUID visualJobId, LocalDateTime fireAt);

    void scheduleLeaseRecovery(UUID visualJobId, LocalDateTime fireAt);

    void scheduleDeadline(UUID parentJobId, LocalDateTime fireAt);

    void cancelChild(UUID visualJobId);

    void cancelParent(UUID parentJobId);
}
