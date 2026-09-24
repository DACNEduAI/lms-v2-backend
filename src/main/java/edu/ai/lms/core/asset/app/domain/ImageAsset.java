package edu.ai.lms.core.asset.app.domain;

import java.util.UUID;

public record ImageAsset(Asset asset, String originalFileName, String contentType, long byteSize, String storageKey,
                         PendingReplacement pendingReplacement) {
    public record PendingReplacement(String originalFileName, String contentType, String storageKey) {}
}
