package edu.ai.lms.core.material.app.port.out.scheduler;

import java.time.Instant;
import java.util.UUID;

public interface ScheduleMaterialIndexJobPort {
    void schedule(UUID jobId, Instant fireAt);
}
