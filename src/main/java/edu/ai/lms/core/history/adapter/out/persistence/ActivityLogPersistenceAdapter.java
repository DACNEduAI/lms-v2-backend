package edu.ai.lms.core.history.adapter.out.persistence;

import edu.ai.lms.core.history.app.domain.ActivityLog;
import edu.ai.lms.core.history.app.port.out.LoadActivityLogPort;
import edu.ai.lms.core.history.app.port.out.SaveActivityLogPort;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityLogPersistenceAdapter implements LoadActivityLogPort, SaveActivityLogPort {

    private final ActivityLogRepository repository;

    @Override
    public void save(ActivityLog activityLog) {
        repository.insert(activityLog);
    }

    @Override
    public List<ActivityLog> findByUserId(UUID userId, int offset, int limit) {
        return repository.findByUserId(userId, offset, limit);
    }

    @Override
    public long countByUserId(UUID userId) {
        return repository.countByUserId(userId);
    }
}