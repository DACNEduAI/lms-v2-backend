package edu.ai.lms.core.coderunner.app.domain;

import java.util.Map;
import java.util.UUID;

/** A contract revision pinned to the database identifiers used by submissions. */
public record StoredCodeRunnerContract(
        UUID configId,
        ExecutableAssessmentContract contract,
        Map<String, UUID> testCaseIds) {
}
