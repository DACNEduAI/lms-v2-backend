package edu.ai.lms.core.quiz.app.port.out;

import edu.ai.lms.core.quiz.app.domain.QuestionLearningOutcomeView;

import java.util.List;
import java.util.UUID;

public interface LoadQuestionLearningOutcomePort {

    List<QuestionLearningOutcomeView> findLearningOutcomesByQuestionId(UUID questionId);

    List<UUID> findLearningOutcomeIdsByQuestionId(UUID questionId);

    List<QuestionLearningOutcomeView> findLearningOutcomesByQuizId(UUID quizId);
}
