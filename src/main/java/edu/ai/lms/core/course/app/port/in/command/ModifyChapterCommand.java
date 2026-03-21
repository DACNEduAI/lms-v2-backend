package edu.ai.lms.core.course.app.port.in.command;

import java.util.UUID;

import edu.ai.lms.core.course.app.domain.Chapter;
import lombok.Builder;

@Builder
public record ModifyChapterCommand(UUID courseId, UUID chapterId, Chapter chapter) {
}
