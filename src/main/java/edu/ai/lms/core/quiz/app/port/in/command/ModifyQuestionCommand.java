package edu.ai.lms.core.quiz.app.port.in.command;

import edu.ai.lms.core.quiz.app.domain.Question;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ModifyQuestionCommand(UUID quizId, UUID questionId, Question question) {
}
