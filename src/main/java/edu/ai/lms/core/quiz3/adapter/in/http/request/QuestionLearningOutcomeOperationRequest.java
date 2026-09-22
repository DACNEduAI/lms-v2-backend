package edu.ai.lms.core.quiz3.adapter.in.http.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionLearningOutcomeOperationRequest {

    @Builder.Default
    @NotNull(message = "learningOutcomeIds is required")
    private List<UUID> learningOutcomeIds = new ArrayList<>();
}
