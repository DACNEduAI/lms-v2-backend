package edu.ai.lms.core.quiz.events;

import java.util.UUID;

public record QuizDeadlineApproachingEvent(
        UUID quizId,
        UUID courseId,
        String quizTitle
) {
}
