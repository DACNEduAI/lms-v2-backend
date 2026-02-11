package edu.ai.lms.core.course.adapter.in.request;

import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Material;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder  // Use @SuperBuilder if there is inheritance

public class CreateMaterialRequest {
    public enum MaterialType {
        PDF,
        VIDEO,
        LINK
    }

    private String title;

    private Material.MaterialType type;

    private String sourceUrl;

    private boolean downloadable;

    private UUID chapter;
}
