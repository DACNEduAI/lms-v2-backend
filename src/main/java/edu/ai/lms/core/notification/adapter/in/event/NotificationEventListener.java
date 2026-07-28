package edu.ai.lms.core.notification.adapter.in.event;

import edu.ai.lms.common.event.SystemNotificationEvent;
import edu.ai.lms.core.course.api.CourseAccessApi;
import edu.ai.lms.core.material.events.MaterialUploadConfirmedEvent;
import edu.ai.lms.core.quiz.events.QuizDeadlineApproachingEvent;
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
    private final CourseAccessApi courseAccessApi;

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

    @org.springframework.modulith.events.ApplicationModuleListener
    public void handleMaterialUploadConfirmed(MaterialUploadConfirmedEvent event) {
        handleSystemNotification(new SystemNotificationEvent(
                new java.util.HashSet<>(courseAccessApi.getStudentIds(event.courseId())),
                "Tài liệu mới!",
                "Giảng viên vừa tải lên tài liệu: " + event.materialTitle(),
                "COURSE_UPDATE",
                "/student/courses/" + event.courseId()));
    }

    @org.springframework.modulith.events.ApplicationModuleListener
    public void handleQuizDeadlineApproaching(QuizDeadlineApproachingEvent event) {
        handleSystemNotification(new SystemNotificationEvent(
                new java.util.HashSet<>(courseAccessApi.getStudentIds(event.courseId())),
                "Nhắc nhở hạn chót",
                "Bài kiểm tra '" + event.quizTitle()
                        + "' sẽ đóng cửa trong vòng 24h tới. Hãy hoàn thành ngay!",
                "DEADLINE_REMINDER",
                "/student/quizzes/" + event.courseId() + "/" + event.quizId() + "/take"));
    }
}
