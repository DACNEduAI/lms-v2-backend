package edu.ai.lms.core.quiz3.app.port.out;

import edu.ai.lms.core.quiz3.app.domain.PendingQuizTarget;
import java.time.LocalDateTime;
import java.util.List;

public interface LoadPendingQuizDeadlinePort {
    List<PendingQuizTarget> findStudentsWithApproachingDeadlines(LocalDateTime fromTime, LocalDateTime toTime);
}
