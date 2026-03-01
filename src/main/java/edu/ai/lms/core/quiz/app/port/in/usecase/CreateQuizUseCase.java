package edu.ai.lms.core.quiz.app.port.in.usecase;

import edu.ai.lms.core.quiz.adapter.in.http.response.CreateQuizResponse;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuizCommand;

public interface CreateQuizUseCase {
    CreateQuizResponse createQuiz(CreateQuizCommand command);
}
