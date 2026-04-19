package edu.ai.lms.core.course.app.port.out;

import java.util.UUID;

import edu.ai.lms.core.course.app.domain.LearningOutcome;

public interface SaveLearningOutcomePort {
    LearningOutcome save(LearningOutcome learningOutcome);
    void assignToGroup(UUID learningOutcomeId, UUID courseId, UUID groupLearningOutcomeId);
    void removeFromGroup(UUID learningOutcomeId, UUID courseId);
}
