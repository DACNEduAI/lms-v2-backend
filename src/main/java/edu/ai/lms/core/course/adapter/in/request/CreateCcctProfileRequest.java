package edu.ai.lms.core.course.adapter.in.request;

import edu.ai.lms.core.course.app.domain.CcctMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCcctProfileRequest(
        @NotBlank(message = "Profile name cannot be blank")
        @Size(max = 255, message = "Profile name cannot exceed 255 characters")
        String name,
        @NotNull(message = "lastMode is required")
        CcctMode lastMode,
        @NotNull(message = "markdown is required")
        String markdown,
        @NotNull(message = "template is required")
        @Valid
        CcctTemplateRequest template) {
}
