package edu.ai.lms.core.learnermodel.app.port.in.usecase;

import edu.ai.lms.core.learnermodel.app.domain.LearnerState;

import java.util.Optional;
import java.util.UUID;

/** Loads the newest immutable learner snapshot for the authenticated student. */
public interface GetLatestLearnerStateUseCase {
    Optional<LearnerState> getLatest(UUID courseId);
}
