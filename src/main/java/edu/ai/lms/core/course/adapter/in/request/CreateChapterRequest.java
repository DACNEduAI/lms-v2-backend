package edu.ai.lms.core.course.adapter.in.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChapterRequest {

    @NotBlank(message = "Chapter title cannot be blank")
    private String title;

    private String description;
}
