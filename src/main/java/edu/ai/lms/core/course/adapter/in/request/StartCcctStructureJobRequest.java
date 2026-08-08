package edu.ai.lms.core.course.adapter.in.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StartCcctStructureJobRequest(
        @NotNull(message = "version is required")
        @PositiveOrZero(message = "version cannot be negative")
        Long version) {
}
