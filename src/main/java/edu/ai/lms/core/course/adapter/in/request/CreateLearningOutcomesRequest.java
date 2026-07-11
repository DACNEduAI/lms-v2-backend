package edu.ai.lms.core.course.adapter.in.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLearningOutcomesRequest {

    @NotEmpty(message = "Learning outcomes list cannot be empty")
    @Valid
    private List<LearningOutcomeItem> learningOutcomes;

    private UUID groupLearningOutcomeId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "CreateLearningOutcomeItemRequest")
    public static class LearningOutcomeItem {
        private Integer prefixNumber;

        @NotBlank(message = "CLO description cannot be blank")
        private String description;
    }
}
