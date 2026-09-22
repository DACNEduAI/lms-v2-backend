package edu.ai.lms.core.quiz3.app.port.in.usecase;

import edu.ai.lms.core.quiz3.adapter.in.http.response.QuizLearningOutcomeOperationResponse;
import edu.ai.lms.core.quiz3.app.port.in.command.RemoveQuizLearningOutcomesCommand;

public interface RemoveQuizLearningOutcomesUseCase {
    QuizLearningOutcomeOperationResponse removeLearningOutcomes(RemoveQuizLearningOutcomesCommand command);
}
