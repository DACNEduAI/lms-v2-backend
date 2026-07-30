package edu.ai.lms.core.course.adapter.in.request;

import java.util.UUID;

import edu.ai.lms.core.course.app.domain.CcctCoverageStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CcctItemRequest(
        UUID id,
        @NotBlank(message = "Item content cannot be blank")
        String content,
        @NotNull(message = "Item status is required")
        CcctCoverageStatus status,
        @Size(max = 2000, message = "Item note cannot exceed 2000 characters")
        String note) {
}
