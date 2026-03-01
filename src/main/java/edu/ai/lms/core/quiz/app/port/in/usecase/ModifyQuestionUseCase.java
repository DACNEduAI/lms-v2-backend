package edu.ai.lms.core.quiz.app.port.in.usecase;

import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyQuestionResponse;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuestionCommand;

public interface ModifyQuestionUseCase {
    ModifyQuestionResponse modifyQuestion(ModifyQuestionCommand command);
}
