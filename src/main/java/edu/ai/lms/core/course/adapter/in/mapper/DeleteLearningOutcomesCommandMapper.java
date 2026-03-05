package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.DeleteLearningOutcomesRequest;
import edu.ai.lms.core.course.app.port.in.command.DeleteLearningOutcomesCommand;

@Component
public class DeleteLearningOutcomesCommandMapper {

    public DeleteLearningOutcomesCommand toCommand(UUID courseId, DeleteLearningOutcomesRequest request) {
        return DeleteLearningOutcomesCommand.builder()
                .courseId(courseId)
                .learningOutcomeIds(request.getLearningOutcomeIds())
                .build();
    }
}
