package edu.ai.lms.core.history.adapter.in.event;

import edu.ai.lms.common.event.SystemActivityEvent;
import edu.ai.lms.core.history.app.port.out.SaveActivityLogPort;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class HistoryEventListenerIT {

    @Test
    void shouldPersistEachSystemActivityEventExactlyOnce() {
        SaveActivityLogPort saveActivityLogPort = mock(SaveActivityLogPort.class);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(SaveActivityLogPort.class, () -> saveActivityLogPort);
            context.scan("edu.ai.lms.core.history.adapter.in.event");
            context.refresh();

            context.publishEvent(SystemActivityEvent.builder()
                    .userId(UUID.randomUUID())
                    .actionType("CREATE")
                    .entityType("CCCT_PROFILE")
                    .entityId(UUID.randomUUID())
                    .entityName("CCCT regression test")
                    .description("One event must create one activity log")
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        verify(saveActivityLogPort, times(1)).save(org.mockito.ArgumentMatchers.any());
    }
}
