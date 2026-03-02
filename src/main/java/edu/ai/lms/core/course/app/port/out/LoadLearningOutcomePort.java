package edu.ai.lms.core.course.app.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.LearningOutcome;

public interface LoadLearningOutcomePort {
    Optional<LearningOutcome> findById(UUID id);
    List<LearningOutcome> findByCourseId(UUID courseId);
}
