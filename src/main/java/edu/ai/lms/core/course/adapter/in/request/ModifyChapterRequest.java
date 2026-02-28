package edu.ai.lms.core.course.adapter.in.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyChapterRequest {

    @NotBlank(message = "Chapter title cannot be blank")
    private String title;

    private String description;

    @Builder.Default
    private List<CreateMaterialRequest> materials = new ArrayList<>();
}
