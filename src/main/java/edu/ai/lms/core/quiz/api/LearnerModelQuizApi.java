package edu.ai.lms.core.quiz.api;

import java.util.List;
import java.util.UUID;

public interface LearnerModelQuizApi {
    List<UUID> findQuizIdsByCourseId(UUID courseId);
}
