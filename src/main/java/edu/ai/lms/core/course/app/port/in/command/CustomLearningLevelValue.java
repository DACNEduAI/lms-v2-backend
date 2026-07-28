package edu.ai.lms.core.course.app.port.in.command;

import java.util.List;
import java.util.UUID;

public record CustomLearningLevelValue(
        UUID id,
        String clientTempId,
        List<UUID> learningOutcomeIds,
        String code,
        String name,
        String description,
        int sortOrder) {
}
