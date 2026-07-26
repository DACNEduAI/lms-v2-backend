package edu.ai.lms.core.quiz.app.domain;

import java.util.UUID;

public record PendingQuizTarget(
    UUID quizId,
    String quizTitle,
    UUID courseId
) {}
