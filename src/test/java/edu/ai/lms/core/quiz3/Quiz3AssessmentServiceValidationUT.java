package edu.ai.lms.core.quiz3;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

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
}
