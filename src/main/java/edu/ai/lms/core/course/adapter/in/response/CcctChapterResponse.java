package edu.ai.lms.core.course.adapter.in.response;

import java.util.List;
import java.util.UUID;

public record CcctChapterResponse(
        UUID id,
        UUID courseChapterId,
        String title,
        String customContent,
        List<CcctTopicResponse> topics) {
}
