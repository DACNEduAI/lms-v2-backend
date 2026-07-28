package edu.ai.lms.core.course.adapter.in.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateExamMatrixRowRequest extends ExamMatrixRowRequest {
    @NotBlank(message = "clientTempId cannot be blank")
    private String clientTempId;
}
