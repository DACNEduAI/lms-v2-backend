package edu.ai.lms.core.quiz.app.port.out;

import java.util.List;
import java.util.UUID;

public record QuizGenerationContextView(
        UUID courseId,
        String courseTitle,
        UUID examMatrixId,
        String matrixTitle,
        List<Row> rows
) {
    public QuizGenerationContextView {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    public record Row(
            UUID rowId,
            UUID learningOutcomeId,
            String learningOutcomeCode,
            String chapterTitle,
            String topic,
            int understandCount,
            int applyCount,
            int applyPlusCount
    ) {
        public int questionCount() {
            return understandCount + applyCount + applyPlusCount;
        }
    }
}
