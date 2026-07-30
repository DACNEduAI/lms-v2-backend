package edu.ai.lms.core.course.app.service.ccct;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import edu.ai.lms.common.event.SystemActivityEvent;
import edu.ai.lms.core.course.app.domain.CcctProfile;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CcctActivity {
    private final ApplicationEventPublisher eventPublisher;

    void publish(UUID actorId, String action, CcctProfile profile) {
        eventPublisher.publishEvent(SystemActivityEvent.builder()
                .userId(actorId)
                .actionType(action)
                .entityType("CCCT_PROFILE")
                .entityId(profile.getId())
                .entityName(profile.getName())
                .description(action + " CCCT profile: " + profile.getName())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
