package edu.ai.lms.core.coderunner.app.port.out;

import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;

import java.util.Optional;
import java.util.UUID;

public interface LoadCodeRunnerContractPort {

    Optional<ExecutableAssessmentContract> findLatestByQuestionId(UUID questionId);
}
