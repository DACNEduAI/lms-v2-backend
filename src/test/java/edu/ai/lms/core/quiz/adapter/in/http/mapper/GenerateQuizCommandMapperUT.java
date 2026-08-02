package edu.ai.lms.core.quiz.adapter.in.http.mapper;

import edu.ai.lms.core.quiz.adapter.in.http.request.GenerateQuizRequest;
import edu.ai.lms.core.quiz.app.port.in.command.GenerateQuizCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateQuizCommandMapperUT {

    private final GenerateQuizCommandMapper mapper = new GenerateQuizCommandMapper();

    @Test
    void mapsConfiguredQuestionDefaults() {
        GenerateQuizRequest request = new GenerateQuizRequest();
        request.setQuestionDefaults(new GenerateQuizRequest.QuestionDefaults(
                new GenerateQuizRequest.MultipleChoiceDefaults(6, true, false)));

        GenerateQuizCommand.MultipleChoiceDefaults defaults = mapper
                .toCommand(request, "key")
                .questionDefaults()
                .multipleChoice();

        assertEquals(6, defaults.answerOptionCount());
        assertTrue(defaults.allowMultipleCorrectAnswers());
        assertFalse(defaults.requireAnswerExplanation());
    }

    @Test
    void normalizesMissingQuestionDefaults() {
        GenerateQuizRequest request = new GenerateQuizRequest();
        request.setQuestionDefaults(null);

        GenerateQuizCommand.MultipleChoiceDefaults defaults = mapper
                .toCommand(request, "key")
                .questionDefaults()
                .multipleChoice();

        assertEquals(4, defaults.answerOptionCount());
        assertFalse(defaults.allowMultipleCorrectAnswers());
        assertTrue(defaults.requireAnswerExplanation());
    }
}
