package edu.ai.lms.core.quiz.app.service.genquiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class QuizGenerationProcessingServiceUT {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldConvertSnapshotRowsToAiPipelineContract() throws Exception {
        UUID learningOutcomeId = UUID.randomUUID();
        JsonNode snapshotRows = objectMapper.readTree("""
                [{
                  "rowId": "b2111111-1111-1111-1111-111111111111",
                  "learningOutcomeId": "%s",
                  "clo": "L.O.1.1",
                  "chapter": "Algorithm Analysis and Big-O",
                  "topic": "Asymptotic complexity and growth-rate comparison",
                  "hieu": 2,
                  "van_dung": 1,
                  "vdc": 1
                }]
                """.formatted(learningOutcomeId));
        QuizGenerationProcessingService service = new QuizGenerationProcessingService(
                null, null, null, null, null, objectMapper);

        QuizGenerationProcessingService.AiExamMatrix result = service.toAiExamMatrix(snapshotRows);
        JsonNode row = result.rows().get(0);

        assertEquals(Set.of("clo", "chapter", "topic", "hieu", "van_dung", "vdc"),
                objectMapper.convertValue(row, java.util.Map.class).keySet());
        assertEquals("L.O.1.1", row.path("clo").asText());
        assertEquals("Algorithm Analysis and Big-O", row.path("chapter").asText());
        assertEquals(2, row.path("hieu").asInt());
        assertEquals(1, row.path("van_dung").asInt());
        assertEquals(1, row.path("vdc").asInt());
        assertEquals(1, result.contractVersion());
        assertEquals(java.util.List.of(learningOutcomeId), result.learningOutcomeIds());
    }

    @Test
    void shouldPreserveNormalizedRowsForVersionTwoContract() throws Exception {
        UUID firstOutcomeId = UUID.randomUUID();
        UUID secondOutcomeId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        JsonNode snapshot = objectMapper.readTree("""
                {
                  "snapshotVersion": 2,
                  "examMatrix": [{
                    "rowId": "%s",
                    "chapterId": "%s",
                    "chapter": "Graphs",
                    "topic": "Shortest paths",
                    "learningOutcomes": [
                      {"id": "%s", "code": "LO1", "description": "Explain"},
                      {"id": "%s", "code": "LO2", "description": "Apply"}
                    ],
                    "levelCounts": [{
                      "customLearningLevelId": "%s",
                      "code": "SYN",
                      "name": "Synthesize",
                      "description": "Combine concepts",
                      "sortOrder": 3,
                      "learningOutcomeIds": ["%s", "%s"],
                      "questionCount": 4
                    }]
                  }]
                }
                """.formatted(rowId, UUID.randomUUID(), firstOutcomeId, secondOutcomeId,
                        levelId, firstOutcomeId, secondOutcomeId));
        QuizGenerationProcessingService service = new QuizGenerationProcessingService(
                null, null, null, null, null, objectMapper);

        QuizGenerationProcessingService.AiExamMatrix result =
                service.toAiExamMatrixFromSnapshot(snapshot);

        assertEquals(2, result.contractVersion());
        assertEquals(java.util.List.of(firstOutcomeId, secondOutcomeId),
                result.learningOutcomeIds());
        assertEquals(rowId.toString(), result.rows().get(0).path("rowId").asText());
        assertEquals(levelId.toString(), result.rows().get(0)
                .path("levelCounts").get(0).path("customLearningLevelId").asText());
        assertEquals(4, result.rows().get(0)
                .path("levelCounts").get(0).path("questionCount").asInt());
    }

    @Test
    void shouldSerializeCanonicalMaterialIdentityFields() {
        UUID courseId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        QuizGenerationProcessingService.AiGenerationRequest request =
                new QuizGenerationProcessingService.AiGenerationRequest(
                        UUID.randomUUID(), 2, courseId, "DSA", "Quiz", objectMapper.createArrayNode(),
                        java.util.List.of(), java.util.List.of(materialId), null, true, "PRESERVE",
                        "idempotency-key", "http://lms/callback", "secret");

        JsonNode json = objectMapper.valueToTree(request);

        assertEquals(courseId.toString(), json.path("course_id").asText());
        assertEquals(materialId.toString(), json.path("material_ids").get(0).asText());
        assertEquals(2, json.path("contractVersion").asInt());
        assertEquals("idempotency-key", json.path("idempotencyKey").asText());
        assertFalse(json.has("courseId"));
        assertFalse(json.has("selectedMaterialIds"));
    }
}
