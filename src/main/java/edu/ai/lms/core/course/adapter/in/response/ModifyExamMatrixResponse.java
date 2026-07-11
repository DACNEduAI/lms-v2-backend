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
 * Response returned after successfully modifying an exam matrix.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyExamMatrixResponse {

    private UUID id;
    private UUID courseId;
    private String title;
    private String description;
    private List<ExamMatrixRowResponse> rows;
    private LocalDateTime updatedAt;
}
