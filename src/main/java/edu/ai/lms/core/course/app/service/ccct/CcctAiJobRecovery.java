package edu.ai.lms.core.course.app.service.ccct;

import java.time.Duration;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.port.out.CcctAiJobPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CcctAiJobRecovery {
    private final CcctAiJobPort jobPort;
    private final CcctAiJobProcessor processor;

    @Scheduled(
            initialDelayString = "${ccct.ai.recovery-initial-delay-ms:5000}",
            fixedDelayString = "${ccct.ai.recovery-delay-ms:60000}")
    void recover() {
        jobPort.requeueStaleRunning(Duration.ofMinutes(20));
        jobPort.findQueuedIds(20).forEach(processor::processAsync);
    }
}
