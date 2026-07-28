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
            UUID chapterId,
            String chapterTitle,
            String topic,
            List<LearningOutcome> learningOutcomes,
            List<LearningLevel> levelCounts
    ) {
        public Row {
            learningOutcomes = learningOutcomes == null ? List.of() : List.copyOf(learningOutcomes);
            levelCounts = levelCounts == null ? List.of() : List.copyOf(levelCounts);
        }

        public int questionCount() {
            return levelCounts.stream().mapToInt(LearningLevel::questionCount).sum();
        }
    }

    public record LearningOutcome(UUID id, String code, String description) {
    }

    public record LearningLevel(
            UUID customLearningLevelId,
            String code,
            String name,
            String description,
            int sortOrder,
            List<UUID> learningOutcomeIds,
            int questionCount
    ) {
        public LearningLevel {
            learningOutcomeIds = learningOutcomeIds == null ? List.of() : List.copyOf(learningOutcomeIds);
        }
    }
}
