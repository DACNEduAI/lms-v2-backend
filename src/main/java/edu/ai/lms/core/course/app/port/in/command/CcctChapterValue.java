package edu.ai.lms.core.course.app.port.in.command;

import java.util.List;
import java.util.UUID;

public record CcctChapterValue(
        UUID id,
        UUID courseChapterId,
        String title,
        List<CcctTopicValue> topics) {

    public CcctChapterValue {
        topics = topics == null ? List.of() : List.copyOf(topics);
    }
}
