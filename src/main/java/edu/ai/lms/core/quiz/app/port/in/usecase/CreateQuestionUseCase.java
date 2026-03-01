package edu.ai.lms.core.quiz.app.port.in.usecase;

import edu.ai.lms.core.quiz.adapter.in.http.response.CreateQuestionResponse;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuestionCommand;

public interface CreateQuestionUseCase {
    CreateQuestionResponse createQuestion(CreateQuestionCommand command);
}
