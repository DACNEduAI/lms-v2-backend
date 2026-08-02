package edu.ai.lms.core.course.app.port.in.command;

import java.util.List;
import java.util.UUID;

import lombok.Builder;

@Builder
public record ExamMatrixRowCommand(
        UUID id,
        String clientTempId,
        UUID chapterId,
        String topic,
        int orderIndex,
        List<UUID> learningOutcomeIds,
        List<LearningLevelCountCommand> levelCounts
) {
    public ExamMatrixRowCommand {
        learningOutcomeIds = learningOutcomeIds == null ? List.of() : List.copyOf(learningOutcomeIds);
        levelCounts = levelCounts == null ? List.of() : List.copyOf(levelCounts);
    }
}
