package edu.ai.lms.core.course.app.port.in.command;

import java.util.UUID;

import lombok.Builder;

@Builder
public record LearningLevelCountCommand(
        UUID customLearningLevelId,
        String customLearningLevelClientTempId,
        int questionCount) {
}
