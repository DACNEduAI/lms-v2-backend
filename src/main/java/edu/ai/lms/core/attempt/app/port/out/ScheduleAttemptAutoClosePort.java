package edu.ai.lms.core.attempt.app.port.out;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ScheduleAttemptAutoClosePort {

    void schedule(UUID attemptId, LocalDateTime fireAt);
}
