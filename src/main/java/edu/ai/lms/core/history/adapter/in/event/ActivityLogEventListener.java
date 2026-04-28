package edu.ai.lms.core.history.adapter.in.event;

import edu.ai.lms.common.event.SystemActivityEvent;
import edu.ai.lms.core.history.app.domain.ActivityLog;
import edu.ai.lms.core.history.app.port.out.SaveActivityLogPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityLogEventListener {

    private final SaveActivityLogPort saveActivityLogPort;

    @Async
    @EventListener
    public void handleSystemActivity(SystemActivityEvent event) {
        log.info("Processing activity log: {} - {}", event.getActionType(), event.getEntityName());
        
        try {
            ActivityLog activityLog = ActivityLog.builder()
                    .userId(event.getUserId())
                    .actionType(event.getActionType())
                    .entityType(event.getEntityType())
                    .entityId(event.getEntityId())
                    .entityName(event.getEntityName())
                    .description(event.getDescription())
                    .createdAt(event.getTimestamp())
                    .build();
            
            saveActivityLogPort.save(activityLog);
        } catch (Exception e) {
            log.error("Failed to save activity log for user {}: {}", event.getUserId(), e.getMessage());
        }
    }
}
