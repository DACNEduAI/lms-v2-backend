package edu.ai.lms.core.course.app.port.out;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.CcctAiJob;

public interface CcctAiJobPort {
    CcctAiJob create(CcctAiJob job);
    Optional<CcctAiJob> findById(UUID jobId);
    boolean claim(UUID jobId);
    void updateStage(UUID jobId, String stage);
    void complete(UUID jobId, List<String> warnings);
    void fail(UUID jobId, String message);
    void stale(UUID jobId, String message);
    int requeueStaleRunning(Duration age);
    List<UUID> findQueuedIds(int limit);
}
