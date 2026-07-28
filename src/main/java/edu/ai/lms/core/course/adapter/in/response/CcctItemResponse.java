package edu.ai.lms.core.course.adapter.in.response;

import java.util.UUID;

import edu.ai.lms.core.course.app.domain.CcctCoverageStatus;

public record CcctItemResponse(
        UUID id,
        String content,
        CcctCoverageStatus status,
        String note) {
}
