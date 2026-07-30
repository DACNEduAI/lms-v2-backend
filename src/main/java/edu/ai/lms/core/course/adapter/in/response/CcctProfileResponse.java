package edu.ai.lms.core.course.adapter.in.response;

import java.time.LocalDateTime;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.CcctMode;
import edu.ai.lms.core.course.app.domain.CcctProfileStatus;

public record CcctProfileResponse(
        UUID id,
        UUID courseId,
        String name,
        CcctProfileStatus status,
        long version,
        CcctMode lastMode,
        String markdown,
        CcctTemplateResponse template,
        UUID createdBy,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
