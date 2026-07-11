package edu.ai.lms.core.course.app.domain;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Domain entity representing a single row in an exam matrix.
 * Each row maps a learning outcome (optionally scoped to a chapter) to
 * Bloom's-taxonomy question counts at three cognitive levels:
 * <ul>
 *   <li>Understand (nhận biết / thông hiểu)</li>
 *   <li>Apply (vận dụng)</li>
 *   <li>Apply+ (vận dụng cao)</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamMatrixRow {

    private UUID id;

    /** The exam matrix this row belongs to. */
    private UUID matrixId;

    /** The learning outcome (CLO) being assessed. */
    private UUID learningOutcomeId;

    /** Optional – scope the row to a specific chapter. */
    private UUID chapterId;

    /** Optional free-text topic label for this row. */
    private String topic;

    @Builder.Default
    private int understandCount = 0;

    @Builder.Default
    private int applyCount = 0;

    @Builder.Default
    private int applyPlusCount = 0;

    /** Display order within the matrix (0-based). */
    private int orderIndex;
}
