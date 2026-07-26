package edu.ai.lms.core.course.api;

import java.util.List;
import java.util.UUID;

public record QuizGenerationContext(
        UUID courseId,
        String courseTitle,
        UUID examMatrixId,
        String matrixTitle,
        List<QuizGenerationRow> rows
) {
    public QuizGenerationContext {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    public record QuizGenerationRow(
            UUID rowId,
            UUID learningOutcomeId,
            String learningOutcomeCode,
            String chapterTitle,
            String topic,
            int understandCount,
            int applyCount,
            int applyPlusCount
    ) {
    }
}
