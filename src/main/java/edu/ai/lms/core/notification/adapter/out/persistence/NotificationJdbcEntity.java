package edu.ai.lms.core.notification.adapter.out.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationJdbcEntity {
    @Id
    private UUID id;
    private UUID userId;
    private String title;
    private String message;
    private String type;
    private String linkUrl;
    private boolean isRead;
    private LocalDateTime createdAt;
}
