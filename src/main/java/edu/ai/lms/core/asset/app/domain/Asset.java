package edu.ai.lms.core.asset.app.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record Asset(UUID id, UUID courseId, Type type, String name, String description, String prompt,
        String purpose, List<String> tags, PublicationStatus publicationStatus, UploadStatus uploadStatus,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
    public enum Type { IMAGE, CODE, FORMULA }
    public enum PublicationStatus { DRAFT, PUBLISHED }
    public enum UploadStatus { PENDING, UPLOADED, FAILED }
}
