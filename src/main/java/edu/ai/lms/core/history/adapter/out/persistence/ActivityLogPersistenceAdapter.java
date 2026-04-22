package edu.ai.lms.core.history.adapter.out.persistence;

import edu.ai.lms.core.history.app.domain.ActivityLog;
import edu.ai.lms.core.history.app.port.out.LoadActivityLogPort;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityLogPersistenceAdapter implements LoadActivityLogPort {

    private final ActivityLogRepository repository;

    @Override
    public List<ActivityLog> findByUserId(UUID userId, int offset, int limit) {
        return repository.findByUserId(userId, offset, limit);
    }

    @Override
    public long countByUserId(UUID userId) {
        return repository.countByUserId(userId);
    }
}