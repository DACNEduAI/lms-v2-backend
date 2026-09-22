package edu.ai.lms.core.quiz3.adapter.in.api;

import edu.ai.lms.core.quiz3.api.LearnerModelQuizApi;
import edu.ai.lms.core.quiz3.app.domain.Quiz;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuizPort;
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
