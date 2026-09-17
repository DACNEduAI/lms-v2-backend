package edu.ai.lms.core.learnermodel.app.port.in.usecase;

import edu.ai.lms.core.learnermodel.app.domain.LearnerState;

import java.util.List;
import java.util.UUID;

public interface GetLearnerStateHistoryUseCase {
    List<LearnerState> getHistory(UUID courseId);
}
