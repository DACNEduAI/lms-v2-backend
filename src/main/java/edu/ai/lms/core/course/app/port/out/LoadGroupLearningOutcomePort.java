package edu.ai.lms.core.course.app.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;

public interface LoadGroupLearningOutcomePort {
    Optional<GroupLearningOutcome> findById(UUID id);
    List<GroupLearningOutcome> findByCourseId(UUID courseId);
    int findMaxSortOrderByCourseId(UUID courseId);
}
