package edu.ai.lms.core.course.app.port.in.command;

import java.util.List;
import java.util.UUID;

import lombok.Builder;

/**
 * Command for creating a new exam matrix under a course.
 */
@Builder
public record CreateExamMatrixCommand(
        UUID courseId,
        UUID ccctProfileId,
        String title,
        String description,
        List<CustomLearningLevelValue> customLearningLevels,
        List<ExamMatrixRowCommand> rows
) {
    public CreateExamMatrixCommand {
        customLearningLevels = customLearningLevels == null ? List.of() : List.copyOf(customLearningLevels);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
