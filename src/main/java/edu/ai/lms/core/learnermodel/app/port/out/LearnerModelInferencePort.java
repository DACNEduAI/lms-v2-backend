package edu.ai.lms.core.learnermodel.app.port.out;

import edu.ai.lms.core.learnermodel.app.domain.LearnerModelType;
import edu.ai.lms.core.learnermodel.app.domain.LearnerQuizHistory;
import edu.ai.lms.core.learnermodel.app.domain.LearningOutcomeTarget;
import edu.ai.lms.core.learnermodel.app.domain.ModelOutcomePrediction;

import java.util.List;
import java.util.UUID;

public interface LearnerModelInferencePort {
    List<ModelOutcomePrediction> predict(
            LearnerModelType model,
            UUID studentId,
            List<LearnerQuizHistory> history,
            List<LearningOutcomeTarget> targets);
}
