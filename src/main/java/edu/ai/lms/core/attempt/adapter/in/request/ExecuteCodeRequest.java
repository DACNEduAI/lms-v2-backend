package edu.ai.lms.core.attempt.adapter.in.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExecuteCodeRequest(
        @NotBlank @Size(max = 100000) String sourceCode) {
}
