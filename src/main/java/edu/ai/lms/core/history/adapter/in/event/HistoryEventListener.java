package edu.ai.lms.core.history.adapter.in.event;

import edu.ai.lms.common.event.SystemActivityEvent;
import edu.ai.lms.core.history.app.domain.ActivityLog;
import edu.ai.lms.core.history.app.port.out.SaveActivityLogPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class HistoryEventListener {

    private final SaveActivityLogPort saveActivityLogPort;

    @Async // Đảm bảo việc ghi log chạy ngầm, không làm chậm thao tác chính của người dùng
    @EventListener
    public void handleSystemActivityEvent(SystemActivityEvent event) {
        try {
            log.info("Received activity event: User {} {} {}", event.getUserId(), event.getActionType(), event.getEntityType());

            ActivityLog logEntry = ActivityLog.builder()
                    .id(UUID.randomUUID())
                    .userId(event.getUserId())
                    .actionType(event.getActionType())
                    .entityType(event.getEntityType())
                    .entityId(event.getEntityId())
                    .entityName(event.getEntityName())
                    .description(event.getDescription())
                    .createdAt(event.getTimestamp())
                    .build();

            saveActivityLogPort.save(logEntry);
            log.info("Activity log saved successfully for user {}", event.getUserId());
        } catch (Exception e) {
            log.error("FAILED to save activity log for user {} - action: {} {}: {}",
                    event.getUserId(), event.getActionType(), event.getEntityType(), e.getMessage(), e);
        }
    }
}
