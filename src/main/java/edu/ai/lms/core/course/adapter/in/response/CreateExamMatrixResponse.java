package edu.ai.lms.core.course.adapter.in.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response returned after successfully creating an exam matrix.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExamMatrixResponse {

    private UUID id;
    private UUID courseId;
    private String title;
    private String description;
    private List<ExamMatrixRowResponse> rows;
    private LocalDateTime createdAt;
}
