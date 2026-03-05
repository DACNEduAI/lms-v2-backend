package edu.ai.lms.core.course.adapter.in.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLearningOutcomesRequest {

    @NotEmpty(message = "Learning outcomes list cannot be empty")
    @Valid
    private List<LearningOutcomeItem> learningOutcomes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LearningOutcomeItem {
        @jakarta.validation.constraints.NotBlank(message = "CLO code cannot be blank")
        private String code;

        @jakarta.validation.constraints.NotBlank(message = "CLO description cannot be blank")
        private String description;
    }
}
