package edu.ai.lms.core.course.app.port.in.command;

import java.util.List;

import edu.ai.lms.core.course.app.domain.CcctMode;

public record CcctProfileContent(
        String name,
        CcctMode lastMode,
        String markdown,
        String templateTitle,
        List<CcctChapterValue> chapters) {

    public CcctProfileContent {
        chapters = chapters == null ? List.of() : List.copyOf(chapters);
    }
}
