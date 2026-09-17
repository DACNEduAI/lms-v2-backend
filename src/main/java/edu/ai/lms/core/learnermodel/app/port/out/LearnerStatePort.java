package edu.ai.lms.core.learnermodel.app.port.out;

import edu.ai.lms.core.learnermodel.app.domain.LearnerState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearnerStatePort {
    boolean existsForAttempt(UUID attemptId);

    boolean saveIfAbsent(LearnerState state);

    List<LearnerState> findByStudentAndCourse(UUID studentId, UUID courseId);

    Optional<LearnerState> findLatestByStudentAndCourse(UUID studentId, UUID courseId);
}
