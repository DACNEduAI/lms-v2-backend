package edu.ai.lms.core.attempt.adapter.in.scheduler;

import edu.ai.lms.core.attempt.app.service.AttemptAutoCloseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttemptAutoCloseScheduler {

    private final AttemptAutoCloseService attemptAutoCloseService;

    @Scheduled(fixedDelay = 1000)
    public void closeExpiredStartedAttemptsScheduler() {
        attemptAutoCloseService.closeAllExpiredStartedAttempts();
    }
}
