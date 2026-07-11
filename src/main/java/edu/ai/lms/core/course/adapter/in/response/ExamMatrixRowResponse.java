package edu.ai.lms.core.course.adapter.in.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for a single exam matrix row.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamMatrixRowResponse {

    private UUID id;
    private UUID learningOutcomeId;
    private UUID chapterId;
    private String topic;
    private int understandCount;
    private int applyCount;
    private int applyPlusCount;
    private int orderIndex;
}
