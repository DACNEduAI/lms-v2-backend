package edu.ai.lms.core.course.app.port.in.command;

import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.LearningOutcome;
import lombok.Builder;

@Builder
public record CreateLearningOutcomesCommand(UUID courseId, List<LearningOutcome> learningOutcomes) {
}
