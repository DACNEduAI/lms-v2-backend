package edu.ai.lms.core.notification.adapter.in.http;

import edu.ai.lms.core.notification.adapter.in.http.request.CourseAnnouncementRequest;
import edu.ai.lms.core.notification.adapter.in.http.response.CourseAnnouncementResponse;
import edu.ai.lms.core.notification.app.domain.CourseAnnouncement;
import edu.ai.lms.core.notification.app.port.in.usecase.GetCourseAnnouncementsUseCase;
import edu.ai.lms.core.notification.app.port.in.usecase.SendCourseAnnouncementUseCase;
import edu.ai.lms.core.auth.adapter.in.internal.LoadAuthenticatedUserApi;
import edu.ai.lms.core.notification.app.domain.Notification;
import edu.ai.lms.core.notification.app.port.in.usecase.NotificationUseCase;
import edu.ai.lms.core.notification.app.service.SseNotificationService;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification", description = "APIs for user notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationUseCase notificationUseCase;
    private final SendCourseAnnouncementUseCase sendCourseAnnouncementUseCase;
    private final GetCourseAnnouncementsUseCase getCourseAnnouncementsUseCase;
    private final LoadAuthenticatedUserApi loadAuthenticatedUserApi;
    private final SseNotificationService sseNotificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to real-time notifications via SSE")
    public SseEmitter stream() {
        try {
            UUID userId = loadAuthenticatedUserApi.getCurrentUserId();
            log.info("User {} subscribing to notification stream", userId);
            return sseNotificationService.subscribe(userId);
        } catch (Exception e) {
            log.error("Failed to subscribe to notification stream", e);
            throw e;
        }
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
        try {
            UUID userId = loadAuthenticatedUserApi.getCurrentUserId();
            log.info("Fetching unread count for user {}", userId);
            long count = notificationUseCase.getUnreadCount(userId);
            return ResponseEntity.ok(Map.of("unreadCount", count));
        } catch (Exception e) {
            log.error("Failed to fetch unread count", e);
            throw e;
        }
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        UUID userId = loadAuthenticatedUserApi.getCurrentUserId();
        notificationUseCase.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all my notifications as read")
    public ResponseEntity<Void> markAllAsRead() {
        UUID userId = loadAuthenticatedUserApi.getCurrentUserId();
        notificationUseCase.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/courses/{courseId}/announce")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Send announcement to all students in a course")
    public ResponseEntity<Void> announceToCourse(
            @PathVariable UUID courseId,
            @RequestBody CourseAnnouncementRequest request) {
        
        sendCourseAnnouncementUseCase.sendAnnouncement(courseId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/courses/{courseId}/announcements")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @Operation(summary = "Get announcement history for a course with pagination")
    public ResponseEntity<Page<CourseAnnouncementResponse>> getAnnouncements(
            @PathVariable UUID courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<CourseAnnouncementResponse> responses = getCourseAnnouncementsUseCase
                .getAnnouncements(courseId, PageRequest.of(page, size))
                .map(announcement -> CourseAnnouncementResponse.builder()
                        .id(announcement.getId())
                        .title(announcement.getTitle())
                        .content(announcement.getContent())
                        .type(announcement.getType())
                        .createdAt(announcement.getCreatedAt())
                        .build());
        
        return ResponseEntity.ok(responses);
    }
}
