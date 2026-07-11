package edu.ai.lms.core.course.app.port.in.command;

import java.util.UUID;

import lombok.Builder;

@Builder
public record ModifyGroupLearningOutcomeCommand(UUID courseId, UUID groupLearningOutcomeId, String description) {
}
