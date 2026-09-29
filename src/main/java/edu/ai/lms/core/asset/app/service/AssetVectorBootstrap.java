package edu.ai.lms.core.asset.app.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.asset.app.domain.ImageAsset;
import edu.ai.lms.core.asset.app.port.out.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Repairs the image-asset vector index after startup without blocking application boot. */
@Component
@RequiredArgsConstructor
@Slf4j
class AssetVectorBootstrap {
    private final AssetRepository assets;
    private final AssetVectorSyncService vectors;
    private final AtomicBoolean complete = new AtomicBoolean(false);

    @Scheduled(initialDelayString = "${asset.vector.bootstrap-initial-delay-ms:5000}", fixedDelayString = "${asset.vector.bootstrap-delay-ms:30000}")
    void syncUntilComplete() {
        if (complete.get()) return;
        try {
            List<ImageAsset> images = assets.findPublishedUploadedImages();
            for (ImageAsset image : images) vectors.upsert(image);
            complete.set(true);
            log.info("event=asset.vector.bootstrap.completed asset_count={}", images.size());
        } catch (Exception exception) {
            log.warn("event=asset.vector.bootstrap.retry error_type={}", exception.getClass().getSimpleName());
        }
    }
}
