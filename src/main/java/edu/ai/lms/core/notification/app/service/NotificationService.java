package edu.ai.lms.core.notification.app.service;

import edu.ai.lms.core.notification.app.domain.Notification;
import edu.ai.lms.core.notification.app.port.in.usecase.NotificationUseCase;
import edu.ai.lms.core.notification.app.port.out.NotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationUseCase {

    private final NotificationPort notificationPort;

    @Override
    public Page<Notification> getMyNotifications(UUID userId, Pageable pageable) {
        return notificationPort.findByUserId(userId, pageable);
    }

    @Override
    public long getUnreadCount(UUID userId) {
        return notificationPort.countUnreadByUserId(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationPort.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        // Bảo mật: Chỉ cho phép người sở hữu đọc thông báo của họ
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to read this notification");
        }

        notification.markAsRead();
        notificationPort.save(notification);
    }

    @Override
    @Transactional
    public Notification createNotification(UUID userId, String title, String message, String type, String linkUrl) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .linkUrl(linkUrl)
                .isRead(false)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        return notificationPort.save(notification);
    }
}
