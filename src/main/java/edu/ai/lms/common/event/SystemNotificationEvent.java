package edu.ai.lms.common.event;

import java.util.Set;
import java.util.UUID;

/**
 * Event dùng để bắn thông báo từ bất kỳ module nào (Course, Quiz...) sang module Notification.
 */
public record SystemNotificationEvent(
    Set<UUID> userIds, // Dùng Set để chứa danh sách ID (ví dụ: thông báo cho toàn bộ sinh viên trong lớp)
    String title,
    String message,
    String type,       // e.g., 'COURSE_UPDATE', 'DEADLINE_REMINDER'
    String linkUrl
) {}
