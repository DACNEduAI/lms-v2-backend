package edu.ai.lms.core.quiz3.app.port.in.usecase;

import edu.ai.lms.core.quiz3.adapter.in.http.response.CreateAnswerResponse;
import edu.ai.lms.core.quiz3.app.port.in.command.CreateAnswerCommand;

public interface CreateAnswerUseCase {
    CreateAnswerResponse createAnswer(CreateAnswerCommand command);
}
