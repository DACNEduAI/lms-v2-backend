package edu.ai.lms.core.quiz3.app.port.in.usecase;

import edu.ai.lms.core.quiz3.adapter.in.http.response.QuestionLearningOutcomeOperationResponse;
import edu.ai.lms.core.quiz3.app.port.in.command.RemoveQuestionLearningOutcomesCommand;

public interface RemoveQuestionLearningOutcomesUseCase {

    QuestionLearningOutcomeOperationResponse removeLearningOutcomes(RemoveQuestionLearningOutcomesCommand command);
}
