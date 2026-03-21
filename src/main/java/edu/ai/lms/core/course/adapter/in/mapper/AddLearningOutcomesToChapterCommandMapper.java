package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.AddLearningOutcomesToChapterRequest;
import edu.ai.lms.core.course.app.port.in.command.AddLearningOutcomesToChapterCommand;

@Component
public class AddLearningOutcomesToChapterCommandMapper {

    public AddLearningOutcomesToChapterCommand toCommand(UUID courseId, UUID chapterId,
                                                         AddLearningOutcomesToChapterRequest request) {
        return AddLearningOutcomesToChapterCommand.builder()
                .courseId(courseId)
                .chapterId(chapterId)
                .learningOutcomeIds(request.getLearningOutcomeIds())
                .build();
    }
}
