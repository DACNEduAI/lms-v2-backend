package edu.ai.lms.core.course.adapter.in.response;

import java.util.List;
import java.util.UUID;

public record CcctProfileListResponse(
        UUID courseId,
        int total,
        List<CcctProfileSummaryResponse> profiles) {
}
