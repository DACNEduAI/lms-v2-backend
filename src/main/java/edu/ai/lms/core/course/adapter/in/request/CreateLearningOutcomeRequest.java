package edu.ai.lms.core.course.adapter.in.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLearningOutcomeRequest {

    private Integer prefixNumber;

    @NotBlank(message = "Learning outcome description cannot be blank")
    private String description;
}
