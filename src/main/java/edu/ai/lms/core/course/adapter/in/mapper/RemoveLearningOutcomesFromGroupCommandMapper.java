package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.RemoveLearningOutcomesFromGroupRequest;
import edu.ai.lms.core.course.app.port.in.command.RemoveLearningOutcomesFromGroupCommand;

@Component
public class RemoveLearningOutcomesFromGroupCommandMapper {

    public RemoveLearningOutcomesFromGroupCommand toCommand(UUID courseId, UUID groupId, RemoveLearningOutcomesFromGroupRequest request) {
        return RemoveLearningOutcomesFromGroupCommand.builder()
                .courseId(courseId)
                .groupLearningOutcomeId(groupId)
                .learningOutcomeIds(request.getLearningOutcomeIds())
                .build();
    }
}
