package edu.ai.lms.core.course.adapter.in.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLearningOutcomeRequest {

    @NotBlank(message = "Learning outcome code cannot be blank")
    private String code;   // e.g. "CLO-1"

    @NotBlank(message = "Learning outcome description cannot be blank")
    private String description;
}
