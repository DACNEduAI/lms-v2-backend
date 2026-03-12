package edu.ai.lms.core.course.adapter.in.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddLearningOutcomesToChapterRequest {

    @NotEmpty(message = "Learning outcome IDs must not be empty")
    private List<UUID> learningOutcomeIds;
}
