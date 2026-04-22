package edu.ai.lms.core.notification.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private UUID id;
    private UUID userId;
    private String title;
    private String message;
    private String type;
    private String linkUrl;
    private boolean isRead;
    private LocalDateTime createdAt;

    // Business Logic: Đánh dấu đã đọc
    public void markAsRead() {
        this.isRead = true;
    }
}
