package edu.ai.lms.core.course.app.port.out;

import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;

import java.util.UUID;

public interface SaveGroupLearningOutcomePort {
    GroupLearningOutcome save(GroupLearningOutcome groupLearningOutcome);
    void updateTreePosition(UUID groupLearningOutcomeId, UUID courseId, String code, int sortOrder);
    void deleteById(UUID groupLearningOutcomeId, UUID courseId);
}
