package edu.ai.lms.core.quiz3.app.domain;

import java.util.UUID;

public record PendingQuizTarget(
    UUID quizId,
    String quizTitle,
    UUID courseId
) {}
