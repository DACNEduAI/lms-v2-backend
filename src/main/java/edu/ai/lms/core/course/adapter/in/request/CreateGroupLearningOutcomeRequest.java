package edu.ai.lms.core.course.adapter.in.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGroupLearningOutcomeRequest {

    @NotBlank(message = "Group name cannot be blank")
    private String name;

    private String description;

    @NotEmpty(message = "Learning outcome IDs list cannot be empty")
    private List<UUID> learningOutcomeIds;
}
