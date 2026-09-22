package edu.ai.lms.core.quiz3.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.quiz3.app.domain.Quiz;

public interface QuizWorkflowUseCase {
    Quiz publish(UUID courseId, UUID quizId, long version);
}
