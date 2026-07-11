package edu.ai.lms.core.course.adapter.in.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Title cannot be blank")
    private String title;

    private String description;

    @Valid
    @Builder.Default
    private List<ExamMatrixRowRequest> rows = new ArrayList<>();
}
