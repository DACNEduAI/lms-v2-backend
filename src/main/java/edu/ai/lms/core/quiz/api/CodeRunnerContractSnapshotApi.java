package edu.ai.lms.core.quiz.api;

import java.util.UUID;

/**
 * Quiz-owned boundary for validating and snapshotting executable contracts.
 * The Code Runner module implements this boundary so Quiz never depends on
 * Code Runner internals.
 */
public interface CodeRunnerContractSnapshotApi {

    String normalizeForReview(String contractJson);

    void createQuestionRevision(UUID questionId, String contractJson);
}
