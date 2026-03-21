package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.ModifyChapterRequest;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.port.in.command.ModifyChapterCommand;

@Component
public class ModifyChapterCommandMapper {

    public ModifyChapterCommand toCommand(UUID courseId, UUID chapterId, ModifyChapterRequest request) {
        Chapter chapter = Chapter.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        return ModifyChapterCommand.builder()
                .courseId(courseId)
                .chapterId(chapterId)
                .chapter(chapter)
                .build();
    }
}
