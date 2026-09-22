package edu.ai.lms.core.quiz3.app.port.in.command;

import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
public record AddQuestionLearningOutcomesCommand(
        UUID quizId,
        UUID questionId,
        List<UUID> learningOutcomeIds
) {
    public AddQuestionLearningOutcomesCommand {
        learningOutcomeIds = learningOutcomeIds == null ? new ArrayList<>() : learningOutcomeIds;
    }
}
