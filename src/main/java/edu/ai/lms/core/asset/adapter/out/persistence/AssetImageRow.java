package edu.ai.lms.core.asset.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Flat MyBatis row for the Asset parent and Image child join. */
@Getter @Setter @NoArgsConstructor
class AssetImageRow {
    private UUID id; private UUID courseId; private String type; private String name; private String description;
    private String prompt; private String tagsJson; private String publicationStatus;
    private String uploadStatus; private LocalDateTime createdAt; private LocalDateTime updatedAt;
    private String originalFileName; private String contentType; private long byteSize; private String storageKey;
    private String pendingOriginalFileName; private String pendingContentType; private String pendingStorageKey;
    private boolean aiGenerated;
}
