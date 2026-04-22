package edu.ai.lms.core.history.app.domain;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {
    private UUID id;
    private UUID userId;
    private String actionType;
    private String entityType;
    private UUID entityId;
    private String entityName;
    private String description;
    private LocalDateTime createdAt;
}