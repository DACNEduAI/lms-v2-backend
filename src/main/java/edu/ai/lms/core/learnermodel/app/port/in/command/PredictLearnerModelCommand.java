package edu.ai.lms.core.learnermodel.app.port.in.command;

import edu.ai.lms.core.learnermodel.app.domain.LearnerModelType;

import java.util.UUID;

public record PredictLearnerModelCommand(
        UUID courseId,
        UUID learningOutcomeId,
        LearnerModelType model
) {
}
