package edu.ai.lms.core.history.app.port.out;

import edu.ai.lms.core.history.app.domain.ActivityLog;
import java.util.List;
import java.util.UUID;

public interface LoadActivityLogPort {
    List<ActivityLog> findByUserId(UUID userId, int offset, int limit);

    long countByUserId(UUID userId);
}