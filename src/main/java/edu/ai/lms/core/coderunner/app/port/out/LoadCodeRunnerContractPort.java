package edu.ai.lms.core.coderunner.app.port.out;

import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import edu.ai.lms.core.coderunner.app.domain.StoredCodeRunnerContract;

import java.util.Optional;
import java.util.UUID;

public interface LoadCodeRunnerContractPort {

    Optional<ExecutableAssessmentContract> findLatestByQuestionId(UUID questionId);

    Optional<StoredCodeRunnerContract> findLatestStoredByQuestionId(UUID questionId);

    Optional<StoredCodeRunnerContract> findReleasedForAttempt(UUID attemptId, UUID questionId);
}
