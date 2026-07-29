package edu.ai.lms.core.course.adapter.in.request;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for creating a new exam matrix.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExamMatrixRequest {

    @NotNull(message = "CCCT profile ID cannot be null")
    private UUID ccctProfileId;

    @NotBlank(message = "Title cannot be blank")
    private String title;

    private String description;

    @Valid
    @Builder.Default
    private List<ExamMatrixRowRequest> rows = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<BatchCreateCustomLearningLevelRequest> customLearningLevels = new ArrayList<>();
}
