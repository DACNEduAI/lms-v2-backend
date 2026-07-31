package edu.ai.lms.core.course.adapter.in.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.CcctAiJobStatus;
import edu.ai.lms.core.course.app.domain.CcctAiJobType;

public record CcctAiJobResponse(
        UUID id,
        UUID courseId,
        UUID profileId,
        CcctAiJobType type,
        CcctAiJobStatus status,
        String stage,
        long profileVersion,
        List<String> warnings,
        String error,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt) {
}
