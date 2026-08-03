package edu.ai.lms.core.quiz.api;

import java.util.Optional;
import java.util.UUID;

public interface CodeRunnerQuestionApi {

    Optional<CodeRunnerQuestionContext> findContext(UUID questionId);
}
