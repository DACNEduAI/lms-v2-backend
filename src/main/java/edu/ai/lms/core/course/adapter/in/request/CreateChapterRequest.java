package edu.ai.lms.core.course.adapter.in.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChapterRequest {

    @NotBlank(message = "Chapter title cannot be blank")
    private String title;

    private String description;

    @Builder.Default
    private List<CreateMaterialRequest> materials = new ArrayList<>();
}
