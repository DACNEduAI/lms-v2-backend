package edu.ai.lms.core.course.app.port.in.command;

import java.util.UUID;

public record CcctItemValue(
        UUID id,
        String contentCode,
        String content) {
    public CcctItemValue(UUID id, String content) {
        this(id, null, content);
    }
}
