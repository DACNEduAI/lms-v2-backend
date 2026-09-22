package edu.ai.lms.core.quiz3.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Quiz-owned boundary for validating and snapshotting executable contracts.
 * The Code Runner module implements this boundary so Quiz never depends on
 * Code Runner internals.
 */
public interface CodeRunnerContractSnapshotApi {

    String normalizeForReview(String contractJson);

    void createQuestionRevision(UUID questionId, String contractJson);

    void cloneLatestQuestionRevision(UUID sourceQuestionId, UUID targetQuestionId);

    /** Student-safe data for printed exams. Hidden tests and reference code are excluded. */
    Optional<PrintableContract> findPrintableByQuestionId(UUID questionId);

    record PrintableContract(
            String statement,
            String entryPoint,
            String starterCode,
            List<PublicExample> publicExamples,
            double points) {
    }

    record PublicExample(Map<String, JsonNode> arguments, JsonNode expectedOutput) {
    }
}
