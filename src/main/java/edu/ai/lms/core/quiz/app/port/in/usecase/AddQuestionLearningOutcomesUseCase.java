package edu.ai.lms.core.quiz.app.port.in.usecase;

import edu.ai.lms.core.quiz.adapter.in.http.response.QuestionLearningOutcomeOperationResponse;
import edu.ai.lms.core.quiz.app.port.in.command.AddQuestionLearningOutcomesCommand;

public interface AddQuestionLearningOutcomesUseCase {

    QuestionLearningOutcomeOperationResponse addLearningOutcomes(AddQuestionLearningOutcomesCommand command);
}
