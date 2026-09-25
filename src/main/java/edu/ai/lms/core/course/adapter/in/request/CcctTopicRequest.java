package edu.ai.lms.core.course.adapter.in.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CcctTopicRequest(
        UUID id,
        @NotBlank(message = "Topic Group ID cannot be blank")
        @Size(max = 64, message = "Topic Group ID cannot exceed 64 characters")
        String groupCode,
        @NotBlank(message = "Topic title cannot be blank")
        @Size(max = 255, message = "Topic title cannot exceed 255 characters")
        String title,
        @NotNull(message = "Topic items are required")
        @Size(max = 500, message = "A topic cannot contain more than 500 items")
        List<@Valid CcctItemRequest> items) {
}
