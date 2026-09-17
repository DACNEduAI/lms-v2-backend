package edu.ai.lms.core.learnermodel.app.domain;

import java.util.UUID;

public record LearnerStateOutcome(
        UUID learningOutcomeId,
        Double probability,
        LearnerStateOutcomeStatus status
) {
}
