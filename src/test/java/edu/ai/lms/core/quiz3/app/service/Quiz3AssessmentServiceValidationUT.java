package edu.ai.lms.core.quiz3.app.service;

import edu.ai.lms.core.quiz3.app.domain.Quiz3Models;
import edu.ai.lms.core.quiz3.adapter.out.persistence.Quiz3RichContentAssetRepository;
import edu.ai.lms.core.course.app.service.matrix.MatrixService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class Quiz3AssessmentServiceValidationUT {
    @Test
    void formatsGroupedLearningOutcomeWithItsGroupCode() {
        assertEquals("L.O.1.1 - Define Big O", Quiz3AssessmentService.formatLearningOutcome("L.O.1", 1, "Define Big O"));
        assertEquals("LO2 - Ungrouped", Quiz3AssessmentService.formatLearningOutcome(null, 2, "Ungrouped"));
    }

    @Test
    void serializesQuestionAssetTypesInTheReviewMetadataContract() {
        var metadata = new Quiz3Models.QuestionMetadata(UUID.randomUUID(), "Chapter", UUID.randomUUID(), "L.O.1.1 - Outcome",
                UUID.randomUUID(), "Understand", "IMAGE", "TEXT", "AUTO_SELECTED", List.of(), List.of());
        var json = new ObjectMapper().valueToTree(metadata);

        assertEquals("IMAGE", json.path("requestedAssetType").asText());
        assertEquals("TEXT", json.path("effectiveAssetType").asText());
    }

    @Test
    void allocatesEveryAssetBucketByLargestRemainderWithStableTies() {
        var coverages = List.of(
                new MatrixService.AssetCoverage(MatrixService.AssetType.TEXT, 25),
                new MatrixService.AssetCoverage(MatrixService.AssetType.IMAGE, 25),
                new MatrixService.AssetCoverage(MatrixService.AssetType.CODE_MCQ, 25),
                new MatrixService.AssetCoverage(MatrixService.AssetType.CODE_RUNNER, 25));

        var allocations = Quiz3AssessmentService.allocateAssetCoverage(2, coverages);

        assertEquals(List.of(1, 1, 0, 0), allocations.stream().map(Quiz3AssessmentService.AssetAllocation::questionCount).toList());
        assertEquals(List.of("TEXT", "IMAGE", "CODE_MCQ", "CODE_RUNNER"), allocations.stream().map(Quiz3AssessmentService.AssetAllocation::assetType).toList());
    }

    @Test
    void countsOnlyAssetAllocationsForTheDsaManualMatrixSnapshot() throws Exception {
        var applyCoverages = List.of(
                new MatrixService.AssetCoverage(MatrixService.AssetType.TEXT, 100),
                new MatrixService.AssetCoverage(MatrixService.AssetType.IMAGE, 0),
                new MatrixService.AssetCoverage(MatrixService.AssetType.CODE_MCQ, 0),
                new MatrixService.AssetCoverage(MatrixService.AssetType.CODE_RUNNER, 0));
        var advancedCoverages = List.of(
                new MatrixService.AssetCoverage(MatrixService.AssetType.TEXT, 50),
                new MatrixService.AssetCoverage(MatrixService.AssetType.IMAGE, 0),
                new MatrixService.AssetCoverage(MatrixService.AssetType.CODE_MCQ, 0),
                new MatrixService.AssetCoverage(MatrixService.AssetType.CODE_RUNNER, 50));
        var mapper = new ObjectMapper();
        UUID applyRowId = UUID.randomUUID(), advancedRowId = UUID.randomUUID();
        UUID applyLevelId = UUID.randomUUID(), advancedLevelId = UUID.randomUUID();
        UUID applyOutcomeId = UUID.randomUUID(), advancedOutcomeId = UUID.randomUUID();
        var snapshot = mapper.readTree("""
                {"matrix":{"rows":[
                  {"id":"%s","learningOutcome":{"id":"%s"},"cells":[
                    {"customLearningLevelId":"%s","questionCount":1,"assetAllocations":%s}
                  ]},
                  {"id":"%s","learningOutcome":{"id":"%s"},"cells":[
                    {"customLearningLevelId":"%s","questionCount":2,"assetAllocations":%s}
                  ]}
                ]}}
                """.formatted(applyRowId, applyOutcomeId, applyLevelId, allocationsJson(mapper, applyCoverages, 1),
                advancedRowId, advancedOutcomeId, advancedLevelId, allocationsJson(mapper, advancedCoverages, 2)));

        assertEquals(3, Quiz3AssessmentService.generatedQuestionTotal(snapshot));
        var allocations = snapshot.findValues("assetAllocations").stream().flatMap(node -> {
            var values = new java.util.ArrayList<JsonNode>();
            node.forEach(values::add);
            return values.stream();
        }).toList();
        assertEquals(2, allocations.stream().filter(value -> value.path("assetType").asText().equals("TEXT")
                && value.path("questionType").asText().equals("MULTIPLE_CHOICE")).mapToInt(value -> value.path("questionCount").asInt()).sum());
        assertEquals(1, allocations.stream().filter(value -> value.path("assetType").asText().equals("CODE_RUNNER")
                && value.path("questionType").asText().equals("CODE_RUNNER")).mapToInt(value -> value.path("questionCount").asInt()).sum());
        assertEquals(0, allocations.stream().filter(value -> value.path("assetType").asText().equals("IMAGE")
                || value.path("assetType").asText().equals("CODE_MCQ")).mapToInt(value -> value.path("questionCount").asInt()).sum());

        var answers = List.of(new Quiz3Models.GeneratedAnswer("A", true, ""), new Quiz3Models.GeneratedAnswer("B", false, ""),
                new Quiz3Models.GeneratedAnswer("C", false, ""), new Quiz3Models.GeneratedAnswer("D", false, ""));
        var questions = List.of(
                new Quiz3Models.GeneratedQuestion("Apply text", "MULTIPLE_CHOICE", applyRowId, applyLevelId, applyOutcomeId, answers, null, null),
                new Quiz3Models.GeneratedQuestion("Advanced text", "MULTIPLE_CHOICE", advancedRowId, advancedLevelId, advancedOutcomeId, answers, null, null),
                new Quiz3Models.GeneratedQuestion("Advanced runner", "CODE_RUNNER", advancedRowId, advancedLevelId, advancedOutcomeId,
                        List.of(), null, mapper.readTree("{\"schema_version\":\"2.0.0\"}"), "CODE_RUNNER", "CODE_RUNNER", null));
        assertDoesNotThrow(() -> Quiz3AssessmentService.validateCallback(snapshot, questions, 3));
    }

    @Test
    void rejectsSnapshotWhoseAssetAllocationsDoNotMatchTheCellQuota() throws Exception {
        var snapshot = new ObjectMapper().readTree("""
                {"matrix":{"rows":[{"cells":[{"questionCount":2,"assetAllocations":[{"questionCount":1}]}]}]}}
                """);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> Quiz3AssessmentService.generatedQuestionTotal(snapshot));
        assertEquals("Quiz3 snapshot asset allocation total does not match cell question count", error.getReason());
    }

    private static String allocationsJson(ObjectMapper mapper, List<MatrixService.AssetCoverage> coverages, int total) {
        var allocations = Quiz3AssessmentService.allocateAssetCoverage(total, coverages);
        var result = mapper.createArrayNode();
        allocations.forEach(allocation -> result.addObject()
                .put("assetType", allocation.assetType())
                .put("questionType", allocation.questionType())
                .put("questionCount", allocation.questionCount()));
        return result.toString();
    }

    @Test
    void acceptsOneCorrectAnswerWithoutAMinimumAnswerCount() {
        assertDoesNotThrow(() -> Quiz3AssessmentService.validateAnswers(
                List.of(new Quiz3Models.AnswerInput("Only answer", "PLAIN_TEXT", true))));
    }

    @Test
    void rejectsZeroOrMultipleCorrectAnswers() {
        assertThrows(ResponseStatusException.class, () -> Quiz3AssessmentService.validateAnswers(List.of()));
        assertThrows(ResponseStatusException.class, () -> Quiz3AssessmentService.validateAnswers(
                List.of(new Quiz3Models.AnswerInput("A", "PLAIN_TEXT", true),
                        new Quiz3Models.AnswerInput("B", "PLAIN_TEXT", true))));
    }

    @Test
    void materializesAutomaticModeRelationsToTheExactStoredTotal() {
        assertEquals(List.of(3, 3, 3, 3, 3), Quiz3AssessmentService.allocateRelationCounts(5, 15));
        assertEquals(List.of(1, 1, 0, 0, 0), Quiz3AssessmentService.allocateRelationCounts(5, 2));
    }

    @Test
    void namesAutomaticModeInAllocationValidationErrors() {
        ResponseStatusException missingRelations = assertThrows(ResponseStatusException.class,
                () -> Quiz3AssessmentService.allocateRelationCounts(0, 1));
        ResponseStatusException missingTotal = assertThrows(ResponseStatusException.class,
                () -> Quiz3AssessmentService.allocateRelationCounts(1, 0));

        assertEquals("Automatic mode needs at least one selected relation", missingRelations.getReason());
        assertEquals("Automatic mode total question count must be greater than zero", missingTotal.getReason());
    }

    @Test
    void buildsQuiz3AiMatrixWithTheUnifiedMatrixContract() {
        UUID matrixId = UUID.randomUUID();
        ObjectNode matrix = Quiz3AssessmentService.createGenerationMatrix(new ObjectMapper(), matrixId, "Matrix");

        assertEquals(matrixId.toString(), matrix.path("id").asText());
        assertEquals("Matrix", matrix.path("name").asText());
        assertEquals(false, matrix.has("type"));
        assertEquals(false, matrix.has("mode"));
        assertEquals(false, matrix.has("version"));
        assertFalse(matrix.has("assetCoverages"));
    }

    @Test
    void serializesTheUnifiedMatrixAssessmentAndContextContracts() {
        UUID assessmentId = UUID.randomUUID(), courseId = UUID.randomUUID(), matrixId = UUID.randomUUID();
        var assessment = new Quiz3Models.AssessmentView(
                assessmentId, courseId, "Assessment", "", false, null, 10.0,
                null, null, null, 1, "DRAFT", 1,
                null, null, matrixId, 4L, Quiz3Models.MatrixMode.MANUAL,
                null, 2L, null, null, null);
        var context = new Quiz3Models.ContextView(
                assessment,
                List.of(new Quiz3Models.SourceOption(UUID.randomUUID(), "Profile", "PUBLISHED", 3L, null)),
                List.of(new Quiz3Models.SourceOption(matrixId, "Matrix", "DRAFT", 4L,
                        Quiz3Models.MatrixMode.MANUAL)));

        var json = new ObjectMapper().valueToTree(context);
        var assessmentJson = json.path("assessment");

        assertTrue(assessmentJson.has("matrixId"));
        assertTrue(assessmentJson.has("matrixVersion"));
        assertTrue(assessmentJson.has("matrixMode"));
        assertFalse(assessmentJson.has("matrix2Id"));
        assertFalse(assessmentJson.has("matrix3Id"));
        assertFalse(assessmentJson.has("matrixSourceType"));
        assertTrue(json.has("matrices"));
        assertFalse(json.has("matrix2s"));
        assertFalse(json.has("matrix3s"));
    }

    @Test
    void validatesCallbackAndCoverageAgainstTheMaterializedSnapshot() throws Exception {
        UUID rowId = UUID.randomUUID(), levelId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        var snapshot = new ObjectMapper().readTree("""
                {"matrix":{"rows":[{"id":"%s","learningOutcome":{"id":"%s"},"cells":[{"customLearningLevelId":"%s","questionCount":2,"assetAllocations":[{"assetType":"TEXT","questionType":"MULTIPLE_CHOICE","questionCount":2}]}]}]}}
                """.formatted(rowId, outcomeId, levelId));
        var answers = List.of(new Quiz3Models.GeneratedAnswer("A", true, ""), new Quiz3Models.GeneratedAnswer("B", false, ""),
                new Quiz3Models.GeneratedAnswer("C", false, ""), new Quiz3Models.GeneratedAnswer("D", false, ""));
        var questions = List.of(new Quiz3Models.GeneratedQuestion("Q1", "MULTIPLE_CHOICE", rowId, levelId, outcomeId, answers, null, null),
                new Quiz3Models.GeneratedQuestion("Q2", "MULTIPLE_CHOICE", rowId, levelId, outcomeId, answers, null, null));
        assertDoesNotThrow(() -> Quiz3AssessmentService.validateCallback(snapshot, questions, 2));
        assertThrows(ResponseStatusException.class, () -> Quiz3AssessmentService.validateCallback(snapshot, questions.subList(0, 1), 1));
        assertEquals(List.of(), Quiz3AssessmentService.coverageDifferences(Map.of("row/level/lo", 2), Map.of("row/level/lo", 2)));
        assertEquals(1, Quiz3AssessmentService.coverageDifferences(Map.of("row/level/lo", 2), Map.of("row/level/lo", 1)).size());
    }

    @Test
    void validatesCodeRunnerAgainstTheQuestionTypeAllocatedByTheMatrix() throws Exception {
        UUID rowId = UUID.randomUUID(), levelId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        var mapper = new ObjectMapper();
        var snapshot = mapper.readTree("""
                {"matrix":{"rows":[{"id":"%s","learningOutcome":{"id":"%s"},"cells":[{
                  "customLearningLevelId":"%s","questionCount":1,"assetAllocations":[{"assetType":"CODE_RUNNER","questionType":"CODE_RUNNER","questionCount":1}]}]}]}}
                """.formatted(rowId, outcomeId, levelId));
        var contract = mapper.readTree("{\"schema_version\":\"2.0.0\"}");
        var codeRunner = new Quiz3Models.GeneratedQuestion("Implement sum", "CODE_RUNNER", rowId, levelId,
                outcomeId, List.of(), null, contract, "CODE_RUNNER", "CODE_RUNNER", null);

        assertDoesNotThrow(() -> Quiz3AssessmentService.validateCallback(snapshot, List.of(codeRunner), 1));
        var wrongType = new Quiz3Models.GeneratedQuestion("Q", "MULTIPLE_CHOICE", rowId, levelId,
                outcomeId, List.of(
                    new Quiz3Models.GeneratedAnswer("A", true, ""), new Quiz3Models.GeneratedAnswer("B", false, ""),
                    new Quiz3Models.GeneratedAnswer("C", false, ""), new Quiz3Models.GeneratedAnswer("D", false, "")),
                null, null);
        assertThrows(ResponseStatusException.class,
                () -> Quiz3AssessmentService.validateCallback(snapshot, List.of(wrongType), 1));
    }
}
