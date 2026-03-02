package edu.ai.lms.core.course.adapter.in.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteLearningOutcomesRequest {

    @NotEmpty(message = "Learning outcome IDs list cannot be empty")
    private List<UUID> learningOutcomeIds;
}
