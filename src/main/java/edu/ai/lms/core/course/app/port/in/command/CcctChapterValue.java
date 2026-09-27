package edu.ai.lms.core.course.app.port.in.command;

import java.util.List;
import java.util.UUID;

public record CcctChapterValue(
        UUID id,
        UUID courseChapterId,
        String chapterCode,
        String title,
        List<CcctItemValue> items) {

    public CcctChapterValue {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
