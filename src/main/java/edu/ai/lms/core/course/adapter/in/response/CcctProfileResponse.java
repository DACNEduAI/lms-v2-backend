package edu.ai.lms.core.course.adapter.in.response;

import java.time.LocalDateTime;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.CcctMode;

public record CcctProfileResponse(
        UUID id,
        UUID courseId,
        String name,
        long version,
        int formatVersion,
        boolean structuredCurrent,
        CcctMode lastMode,
        String markdown,
        CcctTemplateResponse template,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
