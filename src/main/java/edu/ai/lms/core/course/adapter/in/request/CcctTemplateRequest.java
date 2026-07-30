package edu.ai.lms.core.course.adapter.in.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CcctTemplateRequest(
        @NotBlank(message = "Template title cannot be blank")
        @Size(max = 255, message = "Template title cannot exceed 255 characters")
        String title,
        @NotNull(message = "Template chapters are required")
        @Size(max = 100, message = "A profile cannot contain more than 100 chapters")
        List<@Valid CcctChapterRequest> chapters) {
}
