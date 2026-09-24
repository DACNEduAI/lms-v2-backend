package edu.ai.lms.core.course.adapter.in.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record CcctItemRequest(
        UUID id,
        @NotBlank(message = "Item content cannot be blank")
        String content) {
}
