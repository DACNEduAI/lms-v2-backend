package edu.ai.lms.core.course.adapter.in.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record CcctItemRequest(
        UUID id,
        String contentCode,
        @NotBlank(message = "Item content cannot be blank")
        String content) {
    public CcctItemRequest(UUID id, String content) {
        this(id, null, content);
    }
}
