package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.CreateChapterRequest;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.port.in.command.CreateChapterCommand;

@Component
public class CreateChapterCommandMapper {

    public CreateChapterCommand toCommand(UUID courseId, CreateChapterRequest request) {
        Chapter chapter = Chapter.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        return CreateChapterCommand.builder()
                .courseId(courseId)
                .chapter(chapter)
                .build();
    }
}
