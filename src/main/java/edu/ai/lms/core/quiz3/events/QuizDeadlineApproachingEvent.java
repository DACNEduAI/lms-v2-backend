package edu.ai.lms.core.quiz3.events;

import java.util.UUID;

public record QuizDeadlineApproachingEvent(
        UUID quizId,
        UUID courseId,
        String quizTitle
) {
}
