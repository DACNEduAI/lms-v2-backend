package edu.ai.lms.core.quiz.app.port.in.command;

import edu.ai.lms.core.quiz.app.domain.Quiz;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
public record CreateQuizCommand(UUID courseId, Quiz quiz, List<UUID> learningOutcomeIds) {
	public CreateQuizCommand {
		learningOutcomeIds = learningOutcomeIds == null ? new ArrayList<>() : learningOutcomeIds;
	}
}
