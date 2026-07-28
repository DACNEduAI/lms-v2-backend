package edu.ai.lms.core.course.adapter.in.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateExamMatrixRowRequest extends ExamMatrixRowRequest {
    @NotNull(message = "Row ID cannot be null")
    private UUID id;
}
