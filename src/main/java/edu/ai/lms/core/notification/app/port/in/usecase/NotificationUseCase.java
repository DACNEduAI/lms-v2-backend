package edu.ai.lms.core.notification.app.port.in.usecase;

import edu.ai.lms.core.notification.app.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationUseCase {
    Page<Notification> getMyNotifications(UUID userId, Pageable pageable);
    long getUnreadCount(UUID userId);
    void markAsRead(UUID notificationId, UUID userId);
    void markAllAsRead(UUID userId);
    Notification createNotification(UUID userId, String title, String message, String type, String linkUrl);
}
