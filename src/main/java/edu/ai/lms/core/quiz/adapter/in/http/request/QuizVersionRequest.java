package edu.ai.lms.core.quiz.adapter.in.http.request;

import jakarta.validation.constraints.PositiveOrZero;

public record QuizVersionRequest(@PositiveOrZero long version) {
}
