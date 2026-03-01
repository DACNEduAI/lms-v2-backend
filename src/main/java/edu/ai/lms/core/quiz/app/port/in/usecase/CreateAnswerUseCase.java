package edu.ai.lms.core.quiz.app.port.in.usecase;

import edu.ai.lms.core.quiz.adapter.in.http.response.CreateAnswerResponse;
import edu.ai.lms.core.quiz.app.port.in.command.CreateAnswerCommand;

public interface CreateAnswerUseCase {
    CreateAnswerResponse createAnswer(CreateAnswerCommand command);
}
