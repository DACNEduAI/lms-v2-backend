package edu.ai.lms.core.notification.adapter.in.event;

import edu.ai.lms.common.event.SystemNotificationEvent;
import edu.ai.lms.core.notification.app.domain.Notification;
import edu.ai.lms.core.notification.app.service.NotificationService;
import edu.ai.lms.core.notification.app.service.SseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final SseNotificationService sseNotificationService;

    @Async
    @EventListener
    public void handleSystemNotification(SystemNotificationEvent event) {
        log.info("Received system notification event: {} for {} users", event.title(), event.userIds().size());

        // Lặp qua danh sách user cần thông báo (ví dụ: cả lớp học)
        for (UUID userId : event.userIds()) {
            try {
                // 1. Lưu vào Database
                Notification savedNotification = notificationService.createNotification(
                        userId,
                        event.title(),
                        event.message(),
                        event.type(),
                        event.linkUrl());

                // 2. Push Real-time qua SSE nếu user đang online
                sseNotificationService.pushNotification(userId, savedNotification);

            } catch (Exception e) {
                log.error("Error processing notification for user {}: {}", userId, e.getMessage());
            }
        }
    }
}
