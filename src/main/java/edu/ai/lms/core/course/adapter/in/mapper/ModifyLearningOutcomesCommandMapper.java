package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.ModifyLearningOutcomesRequest;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.in.command.ModifyLearningOutcomesCommand;

@Component
public class ModifyLearningOutcomesCommandMapper {

    public ModifyLearningOutcomesCommand toCommand(UUID courseId, ModifyLearningOutcomesRequest request) {
        List<LearningOutcome> learningOutcomes = request.getLearningOutcomes().stream()
                .map(item -> LearningOutcome.builder()
                        .id(item.getId())
                        .code(item.getCode())
                        .description(item.getDescription())
                        .build())
                .collect(Collectors.toList());

        return ModifyLearningOutcomesCommand.builder()
                .courseId(courseId)
                .learningOutcomes(learningOutcomes)
                .build();
    }
}
