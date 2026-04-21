package edu.ai.lms.core.attempt.app.port.out;

import java.util.UUID;

public interface UnscheduleAttemptAutoClosePort {

    void unschedule(UUID attemptId);
}
