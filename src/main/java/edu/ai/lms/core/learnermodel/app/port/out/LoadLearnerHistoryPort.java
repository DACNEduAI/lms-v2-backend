package edu.ai.lms.core.learnermodel.app.port.out;

import edu.ai.lms.core.learnermodel.app.domain.LearnerQuizHistory;

import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

public interface LoadLearnerHistoryPort {
    List<LearnerQuizHistory> load(UUID studentId, UUID courseId);

    List<LearnerQuizHistory> loadThroughAttempt(
            UUID studentId, UUID courseId, UUID attemptId, LocalDateTime submittedAt);
}
