package edu.ai.lms.core.notification.app.port.out;

import edu.ai.lms.core.notification.app.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPort {
    Page<Notification> findByUserId(UUID userId, Pageable pageable);
    long countUnreadByUserId(UUID userId);
    Optional<Notification> findById(UUID id);
    Notification save(Notification notification);
}
