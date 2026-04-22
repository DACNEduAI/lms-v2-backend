package edu.ai.lms.core.notification.adapter.in.http;

import edu.ai.lms.core.auth.adapter.in.internal.LoadAuthenticatedUserApi;
import edu.ai.lms.core.notification.app.domain.Notification;
import edu.ai.lms.core.notification.app.port.in.usecase.NotificationUseCase;
import edu.ai.lms.core.notification.app.service.SseNotificationService;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification", description = "APIs for user notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationUseCase notificationUseCase;
    private final LoadAuthenticatedUserApi loadAuthenticatedUserApi; // Lấy user đang đăng nhập từ auth module
    private final SseNotificationService sseNotificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to real-time notifications via SSE")
    public SseEmitter stream() {
        UUID userId = loadAuthenticatedUserApi.getCurrentUserId();
        return sseNotificationService.subscribe(userId);
    }

    @GetMapping("/me")
    @Operation(summary = "Get my notifications with pagination")
    public ResponseEntity<Page<Notification>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID userId = loadAuthenticatedUserApi.getCurrentUserId();
        Page<Notification> notifications = notificationUseCase.getMyNotifications(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/me/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        UUID userId = loadAuthenticatedUserApi.getCurrentUserId();
        long count = notificationUseCase.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        UUID userId = loadAuthenticatedUserApi.getCurrentUserId();
        notificationUseCase.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }
}
