package edu.ai.lms.core.quiz.adapter.in.http.response;

import java.time.LocalDateTime;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.Quiz;

public record QuizWorkflowResponse(
        UUID quizId,
        String title,
        String lifecycleStatus,
        String sourceType,
        UUID originalQuizId,
        long version,
        boolean display,
        UUID publishedBy,
        LocalDateTime publishedAt) {

    public static QuizWorkflowResponse from(Quiz quiz) {
        return new QuizWorkflowResponse(
                quiz.getId(), quiz.getQuizTitle(), quiz.getLifecycleStatus().name(),
                quiz.getSourceType().name(), quiz.getOriginalQuizId(), quiz.getVersion(),
                Boolean.TRUE.equals(quiz.getDisplay()), quiz.getPublishedBy(), quiz.getPublishedAt());
    }
}
