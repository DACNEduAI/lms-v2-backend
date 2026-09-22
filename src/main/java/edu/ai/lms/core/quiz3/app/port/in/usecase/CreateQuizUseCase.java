package edu.ai.lms.core.quiz3.app.port.in.usecase;

import edu.ai.lms.core.quiz3.adapter.in.http.response.CreateQuizResponse;
import edu.ai.lms.core.quiz3.app.port.in.command.CreateQuizCommand;

public interface CreateQuizUseCase {
    CreateQuizResponse createQuiz(CreateQuizCommand command);
}
