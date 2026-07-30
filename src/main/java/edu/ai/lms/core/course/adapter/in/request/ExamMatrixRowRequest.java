package edu.ai.lms.core.course.adapter.in.request;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamMatrixRowRequest {
    @NotNull(message = "Chapter ID cannot be null")
    private UUID chapterId;

    @NotNull(message = "CCCT item ID cannot be null")
    private UUID ccctItemId;

    private String topic;

    @Min(value = 0, message = "orderIndex must be >= 0")
    private int orderIndex;

    @NotEmpty(message = "At least one learning outcome is required")
    @Builder.Default
    private List<UUID> learningOutcomeIds = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<LearningLevelCountRequest> levelCounts = new ArrayList<>();
}
