package edu.ai.lms.core.quiz.app.port.out;

import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;

public interface LoadQuizGenerationJobPort {
    Optional<QuizGenerationJob> getById(UUID jobId);
}
