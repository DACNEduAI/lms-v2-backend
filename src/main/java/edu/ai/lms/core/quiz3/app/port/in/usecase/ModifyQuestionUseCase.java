package edu.ai.lms.core.quiz3.app.port.in.usecase;

import edu.ai.lms.core.quiz3.adapter.in.http.response.ModifyQuestionResponse;
import edu.ai.lms.core.quiz3.app.port.in.command.ModifyQuestionCommand;

public interface ModifyQuestionUseCase {
    ModifyQuestionResponse modifyQuestion(ModifyQuestionCommand command);
}
