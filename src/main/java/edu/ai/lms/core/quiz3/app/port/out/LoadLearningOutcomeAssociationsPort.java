package edu.ai.lms.core.quiz3.app.port.out;

import edu.ai.lms.core.quiz3.app.domain.QuestionLearningOutcomeLink;

import java.util.List;
import java.util.UUID;

public interface LoadLearningOutcomeAssociationsPort {

    List<UUID> findByQuestionId(UUID questionId);

    List<QuestionLearningOutcomeLink> findByQuizId(UUID quizId);
}
