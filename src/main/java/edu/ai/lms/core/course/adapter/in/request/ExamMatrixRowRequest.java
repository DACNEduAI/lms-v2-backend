package edu.ai.lms.core.course.adapter.in.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for a single row within an exam matrix.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamMatrixRowRequest {

    /** Present on update; null when creating a new row. */
    private UUID id;

    @NotNull(message = "Learning outcome ID cannot be null")
    private UUID learningOutcomeId;

    private UUID chapterId;

    private String topic;

    @Min(value = 0, message = "understandCount must be >= 0")
    private int understandCount;

    @Min(value = 0, message = "applyCount must be >= 0")
    private int applyCount;

    @Min(value = 0, message = "applyPlusCount must be >= 0")
    private int applyPlusCount;

    @Min(value = 0, message = "orderIndex must be >= 0")
    private int orderIndex;
}
