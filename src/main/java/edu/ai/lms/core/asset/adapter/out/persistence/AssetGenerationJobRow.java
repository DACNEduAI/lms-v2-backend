package edu.ai.lms.core.asset.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class AssetGenerationJobRow {
    private UUID id; private UUID assetId; private UUID courseId; private UUID requestedBy;
    private String prompt; private String status; private int attempt; private UUID callbackEventId;
    private String errorMessage; private String errorDetails; private String tikzSource; private String model; private String parametersJson;
    private String generationMode; private String outputStorageKey;
    private LocalDateTime createdAt; private LocalDateTime updatedAt; private LocalDateTime completedAt;
}
