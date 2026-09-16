package edu.ai.lms.core.quiz.adapter.in.api;

import edu.ai.lms.core.quiz.api.LearnerModelQuizApi;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class LearnerModelQuizApiAdapter implements LearnerModelQuizApi {
    private final LoadQuizPort loadQuizPort;

    @Override
    public List<UUID> findQuizIdsByCourseId(UUID courseId) {
        return loadQuizPort.getAllByCourseId(courseId).stream()
                .map(Quiz::getId)
                .toList();
    }
}
