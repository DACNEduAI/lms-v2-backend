package edu.ai.lms.core.quiz.app.port.in.command;

import edu.ai.lms.core.quiz.app.domain.Answer;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ModifyAnswerCommand(UUID questionId, Long answerNumber, Answer answer) {
}
