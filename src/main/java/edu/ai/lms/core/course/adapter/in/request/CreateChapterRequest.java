package edu.ai.lms.core.course.adapter.in.request;

import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.Material;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChapterRequest {
    private String title;
    private String description;
    private Integer orderIndex;

    private Course course;

    @Builder.Default
    private List<CreateMaterialRequest> materials = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
