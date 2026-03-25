package edu.ai.lms.core.quiz.app.port.in.command;

import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
public record RemoveQuestionLearningOutcomesCommand(
        UUID quizId,
        UUID questionId,
        List<UUID> learningOutcomeIds
) {
    public RemoveQuestionLearningOutcomesCommand {
        learningOutcomeIds = learningOutcomeIds == null ? new ArrayList<>() : learningOutcomeIds;
    }
}
