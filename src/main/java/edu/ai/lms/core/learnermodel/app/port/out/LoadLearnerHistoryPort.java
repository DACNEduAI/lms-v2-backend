package edu.ai.lms.core.learnermodel.app.port.out;

import edu.ai.lms.core.learnermodel.app.domain.LearnerQuizHistory;

import java.util.List;
import java.util.UUID;

public interface LoadLearnerHistoryPort {
    List<LearnerQuizHistory> load(UUID studentId, UUID courseId);
}
