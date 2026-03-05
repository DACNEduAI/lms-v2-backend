package edu.ai.lms.core.course.app.port.in.command;

import java.util.UUID;

import edu.ai.lms.core.course.app.domain.LearningOutcome;
import lombok.Builder;

@Builder
public record ModifySingleLearningOutcomeCommand(UUID courseId, UUID learningOutcomeId, LearningOutcome learningOutcome) {
}
