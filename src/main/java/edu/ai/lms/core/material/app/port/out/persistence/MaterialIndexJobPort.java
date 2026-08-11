package edu.ai.lms.core.material.app.port.out.persistence;

import edu.ai.lms.core.material.app.domain.MaterialIndexJob;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaterialIndexJobPort {
    MaterialIndexJob save(MaterialIndexJob job);
    Optional<MaterialIndexJob> findById(UUID jobId);
    boolean claim(UUID jobId, Instant leaseUntil);
    List<UUID> findRecoverableIds(int limit);
}
