package edu.ai.lms.core.material.adapter.in.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.material.app.port.in.usecase.CleanupStaleMaterialsUseCase;
import lombok.RequiredArgsConstructor;

/**
 * Scheduled adapter that periodically triggers the stale-material cleanup use case.
 * Runs every 5 minutes.
 */
@Component
@RequiredArgsConstructor
public class MaterialCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(MaterialCleanupScheduler.class);

    private final CleanupStaleMaterialsUseCase cleanupStaleMaterialsUseCase;

    @Scheduled(fixedRate = 5 * 60 * 1000) // every 5 minutes
    public void runCleanup() {
        log.info("Starting scheduled stale-material cleanup...");
        int deleted = cleanupStaleMaterialsUseCase.cleanupStaleMaterials();
        log.info("Stale-material cleanup finished. Deleted {} row(s).", deleted);
    }
}
