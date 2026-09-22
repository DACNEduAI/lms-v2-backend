package edu.ai.lms.core.quiz3.adapter.in.http.request;

import jakarta.validation.constraints.PositiveOrZero;

public record QuizVersionRequest(@PositiveOrZero long version) {
}
