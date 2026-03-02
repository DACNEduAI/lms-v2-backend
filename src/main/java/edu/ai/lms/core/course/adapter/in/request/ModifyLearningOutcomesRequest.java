package edu.ai.lms.core.course.adapter.in.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyLearningOutcomesRequest {

    @NotEmpty(message = "Learning outcomes list cannot be empty")
    @Valid
    private List<LearningOutcomeItem> learningOutcomes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LearningOutcomeItem {
        @jakarta.validation.constraints.NotNull(message = "Learning outcome ID cannot be null")
        private UUID id;

        private String code;
        private String description;
    }
}
