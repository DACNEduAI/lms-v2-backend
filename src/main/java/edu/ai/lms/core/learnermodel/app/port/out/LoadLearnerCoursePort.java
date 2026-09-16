package edu.ai.lms.core.learnermodel.app.port.out;

import edu.ai.lms.core.learnermodel.app.domain.LearningOutcomeTarget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadLearnerCoursePort {
    boolean isStudentEnrolled(UUID studentId, UUID courseId);

    Optional<LearningOutcomeTarget> findLearningOutcome(UUID courseId, UUID learningOutcomeId);

    List<LearningOutcomeTarget> findLearningOutcomes(UUID courseId);
}
