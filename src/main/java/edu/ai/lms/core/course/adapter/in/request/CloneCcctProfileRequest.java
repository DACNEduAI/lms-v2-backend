package edu.ai.lms.core.course.adapter.in.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CloneCcctProfileRequest(
        @NotBlank(message = "Profile name cannot be blank")
        @Size(max = 255, message = "Profile name cannot exceed 255 characters")
        String name) {
}
