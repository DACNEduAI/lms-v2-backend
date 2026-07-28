package edu.ai.lms.core.course.api;

import java.util.Optional;
import java.util.UUID;

public interface QuizGenerationContextApi {

    Optional<QuizGenerationContext> load(UUID courseId, UUID examMatrixId);
}
