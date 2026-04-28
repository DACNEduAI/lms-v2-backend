package edu.ai.lms.core.quiz.adapter.out.persistence;

import edu.ai.lms.core.quiz.app.domain.PendingQuizTarget;
import edu.ai.lms.core.quiz.app.port.out.LoadPendingQuizDeadlinePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
class QuizDeadlinePersistenceAdapter implements LoadPendingQuizDeadlinePort {

    private final QuizDeadlineRepository repository;

    @Override
    public List<PendingQuizTarget> findStudentsWithApproachingDeadlines(LocalDateTime fromTime, LocalDateTime toTime) {
        return repository.findStudentsWithApproachingDeadlines(fromTime, toTime);
    }
}
