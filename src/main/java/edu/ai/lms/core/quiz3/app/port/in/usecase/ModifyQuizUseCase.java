package edu.ai.lms.core.quiz3.app.port.in.usecase;

import edu.ai.lms.core.quiz3.adapter.in.http.response.ModifyQuizResponse;
import edu.ai.lms.core.quiz3.app.port.in.command.ModifyQuizCommand;

public interface ModifyQuizUseCase {
    ModifyQuizResponse modifyQuiz(ModifyQuizCommand command);
}
