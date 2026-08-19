package edu.ai.lms.core.quiz.adapter.in.api;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.api.QuizReleaseApi;
import edu.ai.lms.core.quiz.app.port.out.ManageQuizReleasePort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuizReleaseApiAdapter implements QuizReleaseApi {
    private final ManageQuizReleasePort releases;

    @Override
    public UUID publish(UUID quizId, long quizVersion, UUID publishedBy, LocalDateTime publishedAt) {
        return releases.publishRelease(quizId, quizVersion, publishedBy, publishedAt);
    }

    @Override
    public void deactivate(UUID quizId) {
        releases.deactivate(quizId);
    }
}
