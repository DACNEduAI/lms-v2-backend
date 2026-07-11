package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.ModifyLearningOutcomeRequest;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.in.command.ModifySingleLearningOutcomeCommand;

@Component
public class ModifySingleLearningOutcomeCommandMapper {

    public ModifySingleLearningOutcomeCommand toCommand(UUID courseId, UUID learningOutcomeId,
            ModifyLearningOutcomeRequest request) {
        LearningOutcome learningOutcome = LearningOutcome.builder()
                .prefixNumber(request.getPrefixNumber())
                .sortOrder(request.getPrefixNumber())
                .description(request.getDescription())
                .build();

        return ModifySingleLearningOutcomeCommand.builder()
                .courseId(courseId)
                .learningOutcomeId(learningOutcomeId)
                .learningOutcome(learningOutcome)
                .build();
    }
}
