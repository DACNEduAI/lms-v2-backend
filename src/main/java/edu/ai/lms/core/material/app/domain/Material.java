package edu.ai.lms.core.material.app.domain;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    public enum MaterialType {
        PDF,
        VIDEO,
        IMAGE
    }

    public enum MaterialStatus {
        PENDING,
        UPLOADED,
        FAILED
    }

    public enum IngestionStatus {
        NOT_STARTED,
        PENDING,
        PROCESSING,
        READY,
        FAILED,
        NOT_APPLICABLE
    }

    private UUID id;
    private String title;
    private MaterialType type;
    private String fileKey;
    private boolean downloadable;
    private MaterialStatus status;
    private IngestionStatus ingestionStatus;
    private String ingestionError;
    private UUID ingestionRevision;
    private LocalDateTime ingestedAt;
    private UUID courseId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- NEW: Dedicated constructor for creating NEW materials ---
    public Material(UUID courseId, String title, MaterialType type, boolean downloadable) {
        this.courseId = courseId;
        this.title = title;
        this.type = type;
        this.downloadable = downloadable;
        this.status = MaterialStatus.PENDING; // Good default for new uploads
        this.ingestionStatus = IngestionStatus.NOT_STARTED;

        // Logic performed explicitly in the constructor
        this.fileKey = generateFileKey(courseId, title, type);
    }

    private String generateFileKey(UUID courseId, String title, MaterialType type) {
        String safeTitle = (title != null) ? title : "untitled";
        String sanitizedTitle = safeTitle.replaceAll("[^a-zA-Z0-9._-]", "_");
        String extension = resolveFileExtension(type, sanitizedTitle);

        // Prevent generating keys like "diagram.png.png" if the title already includes the extension
        if (sanitizedTitle.toLowerCase().endsWith("." + extension)) {
            return String.format("courses/%s/%s", courseId, sanitizedTitle);
        }
        return String.format("courses/%s/%s.%s", courseId, sanitizedTitle, extension);
    }

    private String resolveFileExtension(MaterialType type, String title) {
        if (type == null) return "bin";
        return switch (type) {
            case PDF -> "pdf";
            case VIDEO -> "mp4";
            case IMAGE -> {
                // If the user included the extension in the title, use it.
                // Otherwise, default to png so getContentType() works later.
                String lowerTitle = title.toLowerCase();
                if (lowerTitle.endsWith(".png")) yield "png";
                if (lowerTitle.endsWith(".jpg") || lowerTitle.endsWith(".jpeg")) yield "jpeg";
                if (lowerTitle.endsWith(".gif")) yield "gif";
                if (lowerTitle.endsWith(".webp")) yield "webp";
                if (lowerTitle.endsWith(".svg")) yield "svg";

                yield "png"; // Fallback extension
            }
        };
    }

    public String getContentType() {
        if (this.type == null) return "application/octet-stream";

        return switch (this.type) {
            case PDF -> "application/pdf";
            case VIDEO -> "video/mp4";
            case IMAGE -> resolveImageType(this.fileKey);
        };
    }

    private String resolveImageType(String fileKey) {
        if (fileKey == null) return "application/octet-stream";

        String lowerCaseKey = fileKey.toLowerCase();

        if (lowerCaseKey.endsWith(".png")) return "image/png";
        if (lowerCaseKey.endsWith(".jpg") || lowerCaseKey.endsWith(".jpeg")) return "image/jpeg";
        if (lowerCaseKey.endsWith(".gif")) return "image/gif";
        if (lowerCaseKey.endsWith(".webp")) return "image/webp";
        if (lowerCaseKey.endsWith(".svg")) return "image/svg+xml";

        return "image/*";
    }
}
