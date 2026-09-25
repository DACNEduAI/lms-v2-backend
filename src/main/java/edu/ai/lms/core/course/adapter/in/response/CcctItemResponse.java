package edu.ai.lms.core.course.adapter.in.response;

import java.util.UUID;

public record CcctItemResponse(
        UUID id,
        String contentCode,
        String content) {
    public CcctItemResponse(UUID id, String content) {
        this(id, null, content);
    }
}
