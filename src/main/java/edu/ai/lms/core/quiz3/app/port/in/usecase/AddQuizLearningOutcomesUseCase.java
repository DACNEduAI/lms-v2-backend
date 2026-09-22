package edu.ai.lms.core.quiz3.app.port.in.usecase;

import edu.ai.lms.core.quiz3.adapter.in.http.response.QuizLearningOutcomeOperationResponse;
import edu.ai.lms.core.quiz3.app.port.in.command.AddQuizLearningOutcomesCommand;

public interface AddQuizLearningOutcomesUseCase {
    QuizLearningOutcomeOperationResponse addLearningOutcomes(AddQuizLearningOutcomesCommand command);
}
