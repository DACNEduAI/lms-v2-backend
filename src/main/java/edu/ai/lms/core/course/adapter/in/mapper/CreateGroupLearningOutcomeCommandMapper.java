package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.CreateGroupLearningOutcomeRequest;
import edu.ai.lms.core.course.app.port.in.command.CreateGroupLearningOutcomeCommand;

@Component
public class CreateGroupLearningOutcomeCommandMapper {

    public CreateGroupLearningOutcomeCommand toCommand(UUID courseId, CreateGroupLearningOutcomeRequest request) {
        String code = request.getCode() != null ? request.getCode() : request.getName();
        return CreateGroupLearningOutcomeCommand.builder()
                .courseId(courseId)
                .name(code)
                .description(request.getDescription() != null ? request.getDescription() : code)
                .learningOutcomeIds(request.getLearningOutcomeIds())
                .build();
    }
}
