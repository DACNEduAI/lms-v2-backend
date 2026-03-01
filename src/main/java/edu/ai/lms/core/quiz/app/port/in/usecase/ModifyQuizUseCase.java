package edu.ai.lms.core.quiz.app.port.in.usecase;

import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyQuizResponse;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuizCommand;

public interface ModifyQuizUseCase {
    ModifyQuizResponse modifyQuiz(ModifyQuizCommand command);
}
