package edu.ai.lms.core.course.app.port.in.command;

import java.util.List;
import java.util.UUID;

import lombok.Builder;

@Builder
public record DeleteLearningOutcomesCommand(UUID courseId, List<UUID> learningOutcomeIds) {
}
