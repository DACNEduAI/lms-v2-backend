package edu.ai.lms.core.quiz.app.port.in.usecase;

import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyAnswerResponse;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyAnswerCommand;

public interface ModifyAnswerUseCase {
    ModifyAnswerResponse modifyAnswer(ModifyAnswerCommand command);
}
