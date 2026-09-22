package edu.ai.lms.core.quiz3.app.port.in.command;

import edu.ai.lms.core.quiz3.app.domain.Quiz;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ModifyQuizCommand(UUID courseId, UUID quizId, Quiz quiz, boolean clearAttemptLimit) {
}
