package edu.ai.lms.core.coderunner.api;

import java.util.UUID;

public record CodeSubmissionCompletedEvent(UUID submissionId, UUID attemptId) {
}
