package edu.ai.lms.core.quiz.app.port.in.usecase;

import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;

public interface QuizWorkflowUseCase {
    List<Question> addBankQuestions(UUID courseId, UUID quizId, long version, List<UUID> bankQuestionIds);

    Quiz publish(UUID courseId, UUID quizId, long version);
}
