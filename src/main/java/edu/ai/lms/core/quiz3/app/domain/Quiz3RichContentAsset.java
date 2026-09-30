package edu.ai.lms.core.quiz3.app.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/** Metadata for an object owned by a Quiz3 question only. */
public record Quiz3RichContentAsset(
        UUID id,
        UUID questionId,
        String storageKey,
        String mediaType,
        long byteSize,
        String sha256,
        UUID sourceAssetId,
        UUID aiArtifactId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public Quiz3RichContentAsset {
        if (questionId == null) throw new IllegalArgumentException("A Quiz3 rich content asset requires a question");
    }
}
