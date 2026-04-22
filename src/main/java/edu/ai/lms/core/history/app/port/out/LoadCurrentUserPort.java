package edu.ai.lms.core.history.app.port.out;

import java.util.Optional;
import java.util.UUID;

public interface LoadCurrentUserPort {
    Optional<UUID> getCurrentUserId();
}