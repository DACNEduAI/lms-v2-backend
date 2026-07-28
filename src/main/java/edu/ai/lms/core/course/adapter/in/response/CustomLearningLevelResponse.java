package edu.ai.lms.core.course.adapter.in.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;

@Builder
public record CustomLearningLevelResponse(
        UUID id,
        List<UUID> learningOutcomeIds,
        String code,
        String name,
        String description,
        int sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
