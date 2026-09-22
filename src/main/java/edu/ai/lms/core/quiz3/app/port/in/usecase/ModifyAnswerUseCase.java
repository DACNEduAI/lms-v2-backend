package edu.ai.lms.core.quiz3.app.port.in.usecase;

import edu.ai.lms.core.quiz3.adapter.in.http.response.ModifyAnswerResponse;
import edu.ai.lms.core.quiz3.app.port.in.command.ModifyAnswerCommand;

public interface ModifyAnswerUseCase {
    ModifyAnswerResponse modifyAnswer(ModifyAnswerCommand command);
}
