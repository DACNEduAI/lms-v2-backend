package edu.ai.lms.core.quiz.app.port.in.usecase;

import edu.ai.lms.core.quiz.adapter.in.http.response.QuizLearningOutcomeOperationResponse;
import edu.ai.lms.core.quiz.app.port.in.command.AddQuizLearningOutcomesCommand;

public interface AddQuizLearningOutcomesUseCase {
    QuizLearningOutcomeOperationResponse addLearningOutcomes(AddQuizLearningOutcomesCommand command);
}
