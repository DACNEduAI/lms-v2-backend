package edu.ai.lms.core.quiz.app.domain;

import java.util.UUID;

public record PendingQuizTarget(
    UUID studentId,
    UUID quizId,
    String quizTitle,
    UUID courseId
) {}
