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
    }

    public enum MaterialStatus {
        PENDING,
        UPLOADED,
        FAILED
    }

    private UUID id;

    private String title;

    private MaterialType type;

    private String fileKey;

    private boolean downloadable;

    private MaterialStatus status;

    private UUID courseId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
