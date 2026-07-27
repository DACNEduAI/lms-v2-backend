package edu.ai.lms.core.course.app.port.in.command;

import java.util.List;
import java.util.UUID;

public record BatchEditCustomLearningLevelsCommand(
        UUID courseId,
        List<CustomLearningLevelValue> create,
        List<CustomLearningLevelValue> update,
        List<UUID> delete) {
}
