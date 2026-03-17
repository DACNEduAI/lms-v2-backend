package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.AddLearningOutcomesToGroupRequest;
import edu.ai.lms.core.course.app.port.in.command.AddLearningOutcomesToGroupCommand;

@Component
public class AddLearningOutcomesToGroupCommandMapper {

    public AddLearningOutcomesToGroupCommand toCommand(UUID courseId, UUID groupId, AddLearningOutcomesToGroupRequest request) {
        return AddLearningOutcomesToGroupCommand.builder()
                .courseId(courseId)
                .groupLearningOutcomeId(groupId)
                .learningOutcomeIds(request.getLearningOutcomeIds())
                .build();
    }
}
