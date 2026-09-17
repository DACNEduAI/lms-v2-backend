package edu.ai.lms.core.learnermodel.adapter.in.http.response;

import edu.ai.lms.core.learnermodel.app.domain.LearnerStateOutcomeStatus;

import java.util.UUID;

public record LearnerStateOutcomeResponse(
        UUID learningOutcomeId,
        Double probability,
        LearnerStateOutcomeStatus status
) {
}
