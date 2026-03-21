package edu.ai.lms.core.quiz.app.port.in.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import org.springframework.stereotype.Component;

@Builder
public record RemoveQuizLearningOutcomesCommand(
        UUID courseId,
        UUID quizId,
        List<UUID> learningOutcomeIds,
        List<UUID> groupLearningOutcomeIds
) {
    public RemoveQuizLearningOutcomesCommand {
        learningOutcomeIds = learningOutcomeIds == null ? new ArrayList<>() : learningOutcomeIds;
        groupLearningOutcomeIds = groupLearningOutcomeIds == null ? new ArrayList<>() : groupLearningOutcomeIds;
    }
}
