package edu.ai.lms.core.material.adapter.out.persistence;

import edu.ai.lms.core.material.app.domain.MaterialIndexJob;
import edu.ai.lms.core.material.app.port.out.persistence.MaterialIndexJobPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class MaterialIndexJobPersistenceAdapter implements MaterialIndexJobPort {
    private final MaterialIndexJobRepository repository;

    @Override
    public MaterialIndexJob save(MaterialIndexJob job) {
        if (job.getId() == null) job.setId(UUID.randomUUID());
        repository.save(job);
        return repository.findById(job.getId()).orElseThrow();
    }

    @Override
    public Optional<MaterialIndexJob> findById(UUID jobId) {
        return repository.findById(jobId);
    }

    @Override
    public boolean claim(UUID jobId, LocalDateTime leaseUntil) {
        try {
            return repository.claim(jobId, leaseUntil) == 1;
        } catch (DataIntegrityViolationException ex) {
            // Another revision or delete operation for this material is active.
            return false;
        }
    }

    @Override
    public List<UUID> findRecoverableIds(int limit) {
        return repository.findRecoverableIds(limit);
    }
}
