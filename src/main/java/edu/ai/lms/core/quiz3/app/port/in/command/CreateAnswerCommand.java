package edu.ai.lms.core.quiz3.app.port.in.command;

import edu.ai.lms.core.quiz3.app.domain.Answer;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateAnswerCommand(UUID questionId, Answer answer) {
}
