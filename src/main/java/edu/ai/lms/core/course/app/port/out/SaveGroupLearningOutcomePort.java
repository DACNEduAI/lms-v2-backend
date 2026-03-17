package edu.ai.lms.core.course.app.port.out;

import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;

public interface SaveGroupLearningOutcomePort {
    GroupLearningOutcome save(GroupLearningOutcome groupLearningOutcome);
}
