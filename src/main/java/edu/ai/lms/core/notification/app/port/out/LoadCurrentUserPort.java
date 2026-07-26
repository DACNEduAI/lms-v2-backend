package edu.ai.lms.core.notification.app.port.out;

import java.util.Optional;
import java.util.UUID;

public interface LoadCurrentUserPort {
    Optional<UUID> getCurrentUserId();
}
