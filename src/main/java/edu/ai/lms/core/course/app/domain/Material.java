package edu.ai.lms.core.course.app.domain;

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
        LINK
    }

    private UUID id;

    private String title;

    private MaterialType type;

    private String sourceUrl;

    private boolean downloadable;

    private Chapter chapter;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
