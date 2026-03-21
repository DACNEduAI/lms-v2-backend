package edu.ai.lms.core.quiz.app.port.in.usecase;

import edu.ai.lms.core.quiz.adapter.in.http.response.QuizLearningOutcomeOperationResponse;
import edu.ai.lms.core.quiz.app.port.in.command.RemoveQuizLearningOutcomesCommand;

public interface RemoveQuizLearningOutcomesUseCase {
    QuizLearningOutcomeOperationResponse removeLearningOutcomes(RemoveQuizLearningOutcomesCommand command);
}
