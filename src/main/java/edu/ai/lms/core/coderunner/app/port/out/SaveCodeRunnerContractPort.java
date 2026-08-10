package edu.ai.lms.core.coderunner.app.port.out;

import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;

import java.util.UUID;

public interface SaveCodeRunnerContractPort {

    UUID saveRevision(UUID questionId, ExecutableAssessmentContract contract);

    void markLatestVerified(UUID questionId);
}
