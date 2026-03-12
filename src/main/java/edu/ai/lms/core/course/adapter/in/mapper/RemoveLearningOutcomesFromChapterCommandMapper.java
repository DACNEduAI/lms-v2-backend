package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.RemoveLearningOutcomesFromChapterRequest;
import edu.ai.lms.core.course.app.port.in.command.RemoveLearningOutcomesFromChapterCommand;

@Component
public class RemoveLearningOutcomesFromChapterCommandMapper {

    public RemoveLearningOutcomesFromChapterCommand toCommand(UUID courseId, Long chapterNumber,
                                                               RemoveLearningOutcomesFromChapterRequest request) {
        return RemoveLearningOutcomesFromChapterCommand.builder()
                .courseId(courseId)
                .chapterNumber(chapterNumber)
                .learningOutcomeIds(request.getLearningOutcomeIds())
                .build();
    }
}
