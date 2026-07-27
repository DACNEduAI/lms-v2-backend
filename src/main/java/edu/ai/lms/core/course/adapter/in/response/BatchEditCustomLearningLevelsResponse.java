package edu.ai.lms.core.course.adapter.in.response;

import java.util.List;

import lombok.Builder;

@Builder
public record BatchEditCustomLearningLevelsResponse(
        List<CreatedCustomLearningLevelMappingResponse> createdLevelMappings,
        CustomLearningLevelListResponse result) {
}
