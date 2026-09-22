package edu.ai.lms.core.quiz3.app.port.in.usecase;

import edu.ai.lms.core.quiz3.adapter.in.http.response.CreateQuestionResponse;
import edu.ai.lms.core.quiz3.app.port.in.command.CreateQuestionCommand;

public interface CreateQuestionUseCase {
    CreateQuestionResponse createQuestion(CreateQuestionCommand command);
}
