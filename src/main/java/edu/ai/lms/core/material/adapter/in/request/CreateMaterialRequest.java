package edu.ai.lms.core.material.adapter.in.request;

import edu.ai.lms.core.material.app.domain.Material;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMaterialRequest {

    @NotBlank(message = "Material title cannot be blank")
    private String title;

    @NotNull(message = "Material type cannot be null")
    private Material.MaterialType type; // PDF, VIDEO, LINK

    private Boolean isDownloadable;
}
