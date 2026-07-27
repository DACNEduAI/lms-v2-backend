package edu.ai.lms.core.course.adapter.in.response;

import java.util.List;
import java.util.UUID;

import lombok.Builder;

@Builder
public record CustomLearningLevelListResponse(
        UUID courseId,
        int total,
        List<CustomLearningLevelResponse> levels) {
}
