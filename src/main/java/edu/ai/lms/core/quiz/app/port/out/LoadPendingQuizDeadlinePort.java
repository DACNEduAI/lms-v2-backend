package edu.ai.lms.core.quiz.app.port.out;

import edu.ai.lms.core.quiz.app.domain.PendingQuizTarget;
import java.time.LocalDateTime;
import java.util.List;

public interface LoadPendingQuizDeadlinePort {
    List<PendingQuizTarget> findStudentsWithApproachingDeadlines(LocalDateTime fromTime, LocalDateTime toTime);
}
