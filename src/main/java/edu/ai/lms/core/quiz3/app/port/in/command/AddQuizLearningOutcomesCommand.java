package edu.ai.lms.core.quiz3.app.port.in.command;

import java.util.*;
import java.util.UUID;

import lombok.Builder;
import org.springframework.stereotype.Component;

@Builder
public record AddQuizLearningOutcomesCommand(
        UUID courseId,
        UUID quizId,
        List<UUID> learningOutcomeIds,
        List<UUID> groupLearningOutcomeIds
) {
    public AddQuizLearningOutcomesCommand {
        learningOutcomeIds = learningOutcomeIds == null ? new ArrayList<>() : learningOutcomeIds;
        groupLearningOutcomeIds = groupLearningOutcomeIds == null ? new ArrayList<>() : groupLearningOutcomeIds;
    }
}
