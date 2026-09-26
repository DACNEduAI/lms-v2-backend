package edu.ai.lms.core.course.adapter.in.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CcctChapterRequest(
        UUID id,
        UUID courseChapterId,
        @NotBlank(message = "Chapter Code cannot be blank")
        @Size(max = 64, message = "Chapter Code cannot exceed 64 characters")
        String chapterCode,
        @NotBlank(message = "Chapter title cannot be blank")
        @Size(max = 255, message = "Chapter title cannot exceed 255 characters")
        String title,
        @NotNull(message = "Chapter content details are required")
        @Size(max = 500, message = "A chapter cannot contain more than 500 content details")
        List<@Valid CcctItemRequest> items) {
}
