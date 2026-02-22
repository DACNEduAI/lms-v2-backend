package edu.ai.lms.core.course.adapter.in.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChapterRequest {

    @NotBlank(message = "Chapter title cannot be blank")
    private String title;

    private String description;

    private Integer orderIndex;

    @NotNull(message = "Course ID cannot be null")
    private UUID courseId;

    @Builder.Default
    private List<CreateMaterialRequest> materials = new ArrayList<>();
}
