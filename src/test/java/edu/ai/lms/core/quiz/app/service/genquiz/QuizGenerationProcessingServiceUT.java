package edu.ai.lms.core.quiz.app.service.genquiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.ScheduleQuizGenerationJobPort;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.time.Clock;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                null, null, null, null, null, objectMapper, null, Clock.systemUTC());

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
                null, null, null, null, null, objectMapper, null, Clock.systemUTC());

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
    void shouldPreserveMatrixRowsForVersionFourContract() throws Exception {
        UUID outcomeId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        JsonNode snapshot = objectMapper.readTree("""
                {
                  "snapshotVersion": 4,
                  "examMatrix": [{
                    "rowId": "%s",
                    "content": "Independent matrix topic",
                    "learningOutcomes": [
                      {"id": "%s", "code": "LO1", "description": "Explain"}
                    ],
                    "levelCounts": [{
                      "customLearningLevelId": "%s",
                      "learningOutcomeIds": ["%s"],
                      "questionType": "MULTIPLE_CHOICE",
                      "questionCount": 2
                    }]
                  }]
                }
                """.formatted(rowId, outcomeId, levelId, outcomeId));
        QuizGenerationProcessingService service = new QuizGenerationProcessingService(
                null, null, null, null, null, objectMapper, null, Clock.systemUTC());

        QuizGenerationProcessingService.AiExamMatrix result =
                service.toAiExamMatrixFromSnapshot(snapshot);

        assertEquals(4, result.contractVersion());
        assertFalse(result.rows().get(0).has("ccctItemId"));
        assertEquals("MULTIPLE_CHOICE", result.rows().get(0)
                .path("levelCounts").get(0).path("questionType").asText());
    }

    @Test
    void shouldSerializeCanonicalMaterialIdentityFields() {
        UUID courseId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        QuizGenerationProcessingService.AiGenerationRequest request =
                new QuizGenerationProcessingService.AiGenerationRequest(
                        UUID.randomUUID(), 2, courseId, "DSA", "Quiz", objectMapper.createArrayNode(),
                        java.util.List.of(), java.util.List.of(materialId), null,
                        new edu.ai.lms.core.quiz.app.port.in.command.GenerateQuizCommand.QuestionDefaultsConfiguration(
                                new edu.ai.lms.core.quiz.app.port.in.command.GenerateQuizCommand.MultipleChoiceDefaults(
                                        6, true, false)),
                        true, "PRESERVE",
                        "idempotency-key", "http://lms/callback", "secret");

        JsonNode json = objectMapper.valueToTree(request);

        assertEquals(courseId.toString(), json.path("course_id").asText());
        assertEquals(materialId.toString(), json.path("material_ids").get(0).asText());
        assertEquals(2, json.path("contractVersion").asInt());
        assertEquals("idempotency-key", json.path("idempotencyKey").asText());
        assertFalse(json.has("courseId"));
        assertFalse(json.has("selectedMaterialIds"));
        assertEquals(6, json.path("questionDefaultsConfiguration").path("multipleChoice")
                .path("answerOptionCount").asInt());
        assertTrue(json.path("questionDefaultsConfiguration").path("multipleChoice")
                .path("allowMultipleCorrectAnswers").asBoolean());
        assertFalse(json.path("questionDefaultsConfiguration").path("multipleChoice")
                .path("requireAnswerExplanation").asBoolean());
    }

    @Test
    void shouldDefaultQuestionDefaultsConfigurationForOlderSnapshots() throws Exception {
        JsonNode snapshot = objectMapper.readTree("""
                {"snapshotVersion": 4}
                """);
        QuizGenerationProcessingService service = new QuizGenerationProcessingService(
                null, null, null, null, null, objectMapper, null, Clock.systemUTC());

        var defaults = service.questionDefaultsConfigurationFromSnapshot(snapshot).multipleChoice();

        assertEquals(4, defaults.answerOptionCount());
        assertFalse(defaults.allowMultipleCorrectAnswers());
        assertTrue(defaults.requireAnswerExplanation());
    }

    @Test
    void freshJobIsClaimedEvenWhenTheJvmTimezoneIsAsiaHoChiMinh() {
        Instant now = Instant.parse("2026-08-13T19:37:05Z");
        QuizGenerationJob job = QuizGenerationJob.builder().id(UUID.randomUUID())
                .status(QuizGenerationStatus.PENDING).createdAt(now.minusSeconds(6)).build();
        LoadQuizGenerationJobPort load = mock(LoadQuizGenerationJobPort.class);
        SaveQuizGenerationJobPort save = mock(SaveQuizGenerationJobPort.class);
        when(load.getById(job.getId())).thenReturn(Optional.of(job));
        when(save.claim(eq(job.getId()), any())).thenReturn(false);
        QuizGenerationProcessingService service = new QuizGenerationProcessingService(
                load, save, mock(ScheduleQuizGenerationJobPort.class), mock(SseQuizGenerationService.class), null,
                objectMapper, mock(QuizGenerationTimingLogger.class), Clock.fixed(now, ZoneOffset.UTC));
        ReflectionTestUtils.setField(service, "timeoutMinutes", 20L);

        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            service.process(job.getId());
        } finally {
            TimeZone.setDefault(original);
        }

        verify(save).claim(job.getId(), now.plusSeconds(20 * 60));
        verify(save, never()).save(any());
        assertEquals(QuizGenerationStatus.PENDING, job.getStatus());
    }

    @Test
    void trulyExpiredJobTimesOutBeforeItIsClaimed() {
        Instant now = Instant.parse("2026-08-13T19:37:05Z");
        QuizGenerationJob job = QuizGenerationJob.builder().id(UUID.randomUUID())
                .status(QuizGenerationStatus.PENDING).createdAt(now.minusSeconds(20 * 60 + 1)).build();
        LoadQuizGenerationJobPort load = mock(LoadQuizGenerationJobPort.class);
        SaveQuizGenerationJobPort save = mock(SaveQuizGenerationJobPort.class);
        when(load.getById(job.getId())).thenReturn(Optional.of(job));
        when(save.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        QuizGenerationProcessingService service = new QuizGenerationProcessingService(
                load, save, mock(ScheduleQuizGenerationJobPort.class), mock(SseQuizGenerationService.class), null,
                objectMapper, mock(QuizGenerationTimingLogger.class), Clock.fixed(now, ZoneOffset.UTC));
        ReflectionTestUtils.setField(service, "timeoutMinutes", 20L);

        service.process(job.getId());

        assertEquals(QuizGenerationStatus.TIMED_OUT, job.getStatus());
        verify(save, never()).claim(any(), any());
    }
}
