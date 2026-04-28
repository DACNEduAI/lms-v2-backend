package edu.ai.lms.core.history.app.port.out;

import edu.ai.lms.core.history.app.domain.ActivityLog;

public interface SaveActivityLogPort {
    void save(ActivityLog activityLog);
}
