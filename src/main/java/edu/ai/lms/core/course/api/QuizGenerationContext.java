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
            UUID chapterId,
            String chapterTitle,
            String topic,
            List<LearningOutcomeRequirement> learningOutcomes,
            List<LearningLevelRequirement> levelCounts
    ) {
        public QuizGenerationRow {
            learningOutcomes = learningOutcomes == null ? List.of() : List.copyOf(learningOutcomes);
            levelCounts = levelCounts == null ? List.of() : List.copyOf(levelCounts);
        }
    }

    public record LearningOutcomeRequirement(
            UUID id,
            String code,
            String description
    ) {
    }

    public record LearningLevelRequirement(
            UUID customLearningLevelId,
            String code,
            String name,
            String description,
            int sortOrder,
            List<UUID> learningOutcomeIds,
            int questionCount
    ) {
        public LearningLevelRequirement {
            learningOutcomeIds = learningOutcomeIds == null ? List.of() : List.copyOf(learningOutcomeIds);
        }
    }
}
