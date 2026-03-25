package edu.ai.lms.core.quiz.app.port.in.usecase;

import edu.ai.lms.core.quiz.adapter.in.http.response.QuestionLearningOutcomeOperationResponse;
import edu.ai.lms.core.quiz.app.port.in.command.RemoveQuestionLearningOutcomesCommand;

public interface RemoveQuestionLearningOutcomesUseCase {

    QuestionLearningOutcomeOperationResponse removeLearningOutcomes(RemoveQuestionLearningOutcomesCommand command);
}
