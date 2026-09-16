package edu.ai.lms.core.attempt.api;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LearnerHistoryApi {
    List<LearnerQuizHistoryView> findFinishedHistory(UUID studentId, Collection<UUID> quizIds);
}
