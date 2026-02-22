package edu.ai.lms.core.course.app.port.in.command;

import edu.ai.lms.core.course.app.domain.Chapter;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateChapterCommand(UUID courseId, Chapter chapter) {
}
