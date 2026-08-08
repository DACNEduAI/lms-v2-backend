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
        @NotBlank(message = "Chapter title cannot be blank")
        @Size(max = 255, message = "Chapter title cannot exceed 255 characters")
        String title,
        String customContent,
        @NotNull(message = "Chapter topics are required")
        @Size(max = 200, message = "A chapter cannot contain more than 200 topics")
        List<@Valid CcctTopicRequest> topics) {
}
