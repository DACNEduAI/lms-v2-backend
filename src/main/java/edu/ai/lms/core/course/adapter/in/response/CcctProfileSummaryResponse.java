package edu.ai.lms.core.course.adapter.in.response;

import java.time.LocalDateTime;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.CcctMode;
import edu.ai.lms.core.course.app.domain.CcctProfileStatus;

public record CcctProfileSummaryResponse(
        UUID id,
        String name,
        CcctProfileStatus status,
        long version,
        int formatVersion,
        CcctMode lastMode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
