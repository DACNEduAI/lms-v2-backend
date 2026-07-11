package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.CreateGroupLearningOutcomeRequest;
import edu.ai.lms.core.course.app.port.in.command.CreateGroupLearningOutcomeCommand;

@Component
public class CreateGroupLearningOutcomeCommandMapper {

    public CreateGroupLearningOutcomeCommand toCommand(UUID courseId, CreateGroupLearningOutcomeRequest request) {
        return CreateGroupLearningOutcomeCommand.builder()
                .courseId(courseId)
                .name(request.getName())
                .description(request.getDescription() != null ? request.getDescription() : request.getName())
                .learningOutcomeIds(request.getLearningOutcomeIds())
                .build();
    }
}
