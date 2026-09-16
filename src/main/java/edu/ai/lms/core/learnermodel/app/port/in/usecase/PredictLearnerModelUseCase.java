package edu.ai.lms.core.learnermodel.app.port.in.usecase;

import edu.ai.lms.core.learnermodel.app.domain.LearnerModelPredictionResult;
import edu.ai.lms.core.learnermodel.app.port.in.command.PredictLearnerModelCommand;

public interface PredictLearnerModelUseCase {
    LearnerModelPredictionResult predictSingle(PredictLearnerModelCommand command);

    LearnerModelPredictionResult predictAll(PredictLearnerModelCommand command);
}
