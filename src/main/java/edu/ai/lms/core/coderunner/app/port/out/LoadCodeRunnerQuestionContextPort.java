package edu.ai.lms.core.coderunner.app.port.out;

import edu.ai.lms.core.coderunner.app.domain.CodeRunnerQuestionContext;

import java.util.Optional;
import java.util.UUID;

public interface LoadCodeRunnerQuestionContextPort {

    Optional<CodeRunnerQuestionContext> findQuestionContext(UUID questionId);
}
