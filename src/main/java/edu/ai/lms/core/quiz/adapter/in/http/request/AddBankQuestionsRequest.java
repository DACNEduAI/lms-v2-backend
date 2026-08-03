package edu.ai.lms.core.quiz.adapter.in.http.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

public record AddBankQuestionsRequest(
        @PositiveOrZero long version,
        @NotEmpty List<UUID> bankQuestionIds) {
}
