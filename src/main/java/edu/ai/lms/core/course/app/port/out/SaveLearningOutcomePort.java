package edu.ai.lms.core.course.app.port.out;

import edu.ai.lms.core.course.app.domain.LearningOutcome;

public interface SaveLearningOutcomePort {
    LearningOutcome save(LearningOutcome learningOutcome);
}
