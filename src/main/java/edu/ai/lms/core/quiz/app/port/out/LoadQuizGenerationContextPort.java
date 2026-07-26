package edu.ai.lms.core.quiz.app.port.out;

import java.util.Optional;
import java.util.UUID;

public interface LoadQuizGenerationContextPort {

    Optional<QuizGenerationContextView> load(UUID courseId, UUID examMatrixId);
}
