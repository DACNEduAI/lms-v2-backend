package edu.ai.lms.core.quiz3;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class Quiz3AssessmentServiceValidationUT {
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
    void materializesMatrix3RelationsToTheExactStoredTotal() {
        assertEquals(List.of(3, 3, 3, 3, 3), Quiz3AssessmentService.allocateRelationCounts(5, 15));
        assertEquals(List.of(1, 1, 0, 0, 0), Quiz3AssessmentService.allocateRelationCounts(5, 2));
    }

    @Test
    void buildsQuiz3AiMatrixWithoutTheInternalMatrixType() {
        UUID matrixId = UUID.randomUUID();
        ObjectNode matrix = Quiz3AssessmentService.createGenerationMatrix(new ObjectMapper(), matrixId, "Matrix 3");

        assertEquals(matrixId.toString(), matrix.path("id").asText());
        assertEquals("Matrix 3", matrix.path("name").asText());
        assertEquals(false, matrix.has("type"));
    }

    @Test
    void validatesCallbackAndCoverageAgainstTheMaterializedSnapshot() throws Exception {
        UUID rowId = UUID.randomUUID(), levelId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        var snapshot = new ObjectMapper().readTree("""
                {"matrix":{"rows":[{"id":"%s","learningOutcome":{"id":"%s"},"cells":[{"customLearningLevelId":"%s","questionCount":2}]}]}}
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
                  "customLearningLevelId":"%s","questionCount":1,"questionType":"CODE_RUNNER"}]}]}}
                """.formatted(rowId, outcomeId, levelId));
        var contract = mapper.readTree("{\"schema_version\":\"2.0.0\"}");
        var codeRunner = new Quiz3Models.GeneratedQuestion("Implement sum", "CODE_RUNNER", rowId, levelId,
                outcomeId, List.of(), null, contract);

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
