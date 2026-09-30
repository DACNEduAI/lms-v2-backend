package edu.ai.lms.core.course.adapter.in.response;

import java.time.LocalDateTime;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.CcctMode;

public record CcctProfileSummaryResponse(
        UUID id,
        String name,
        long version,
        int formatVersion,
        CcctMode lastMode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
