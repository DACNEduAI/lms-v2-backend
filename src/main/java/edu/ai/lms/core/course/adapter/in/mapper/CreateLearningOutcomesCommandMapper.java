package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.CreateLearningOutcomesRequest;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.in.command.CreateLearningOutcomesCommand;

@Component
public class CreateLearningOutcomesCommandMapper {

    public CreateLearningOutcomesCommand toCommand(UUID courseId, CreateLearningOutcomesRequest request) {
        List<LearningOutcome> learningOutcomes = request.getLearningOutcomes().stream()
                .map(item -> LearningOutcome.builder()
                        .description(item.getDescription())
                        .build())
                .collect(Collectors.toList());

        return CreateLearningOutcomesCommand.builder()
                .courseId(courseId)
                .groupLearningOutcomeId(request.getGroupLearningOutcomeId())
                .learningOutcomes(learningOutcomes)
                .build();
    }
}
