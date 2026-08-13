package edu.ai.lms.core.quiz.app.service.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository.AnswerInput;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AssessmentView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantQuestionMetadata;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantQuestionView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantView;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService.VariantEditingContext;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService.VariantQuestionInput;
import edu.ai.lms.core.quiz.app.service.assessment.QuestionRefinementService.RefinementRequest;
import edu.ai.lms.core.quiz.app.service.assessment.QuestionRefinementService.RefinementResponse;
import edu.ai.lms.core.quiz.app.service.assessment.QuestionRefinementService.Target;

class QuestionRefinementServiceUT {
    private static final UUID COURSE_ID = UUID.randomUUID();
    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();
    private static final UUID QUESTION_ID = UUID.randomUUID();

    @Test
    void acceptsAProviderMockResponseWithoutPersistingTheDraft() {
        AssessmentService assessments = assessments();
        QuestionRefinementService service = service(assessments, response("QUESTION", "Clear question", "B"));

        var result = service.refine(COURSE_ID, ASSESSMENT_ID, VARIANT_ID, QUESTION_ID,
                new RefinementRequest("Rewrite clearly", Target.QUESTION, draft(), List.of()));

        assertEquals("Clear question", result.draft().content());
        assertEquals("B", result.draft().answers().get(1).content());
        assertEquals(List.of(Target.QUESTION), result.changedTargets());
    }

    @Test
    void rejectsProviderChangesOutsideTheSelectedTarget() {
        AssessmentService assessments = assessments();
        QuestionRefinementService service = service(assessments, response("QUESTION", "Clear question", "Changed B"));

        assertThrows(IllegalArgumentException.class, () -> service.refine(
                COURSE_ID, ASSESSMENT_ID, VARIANT_ID, QUESTION_ID,
                new RefinementRequest("Rewrite clearly", Target.QUESTION, draft(), List.of())));
    }

    @Test
    void calculatesChangedTargetsInsteadOfTrustingTheProvider() {
        QuestionRefinementService service = service(assessments(), response("EXPLANATION", "Clear question", "B"));

        RefinementResponse result = service.refine(COURSE_ID, ASSESSMENT_ID, VARIANT_ID, QUESTION_ID,
                new RefinementRequest("Rewrite clearly", Target.QUESTION, draft(), List.of()));

        assertEquals(List.of(Target.QUESTION), result.changedTargets());
    }

    @Test
    void rejectsDifferentSvgSourceAndBase64Payload() {
        String response = response("QUESTION", "Clear question", "B").replace(
                "\"visual\": null",
                """
                \"visual\": {
                  \"id\": \"00000000-0000-0000-0000-000000000001\",
                  \"target\": \"QUESTION\",
                  \"altText\": \"Diagram\",
                  \"sourceCode\": \"<svg viewBox='0 0 10 10'><circle cx='5' cy='5' r='2'/></svg>\",
                  \"renderedImageBase64\": \"PHN2ZyB2aWV3Qm94PScwIDAgMTAgMTAnPjxyZWN0IHdpZHRoPScxMCcgaGVpZ2h0PScxMCcvPjwvc3ZnPg==\",
                  \"renderedMediaType\": \"image/svg+xml\"
                }
                """);
        QuestionRefinementService service = service(assessments(), response);

        assertThrows(IllegalArgumentException.class, () -> service.refine(
                COURSE_ID, ASSESSMENT_ID, VARIANT_ID, QUESTION_ID,
                new RefinementRequest("Add a diagram", Target.QUESTION, draft(), List.of())));
    }

    private AssessmentService assessments() {
        AssessmentService assessments = mock(AssessmentService.class);
        AssessmentView assessment = mock(AssessmentView.class);
        VariantView variant = mock(VariantView.class);
        VariantQuestionView question = mock(VariantQuestionView.class);
        when(assessment.optionCount()).thenReturn(4);
        when(question.metadata()).thenReturn(new VariantQuestionMetadata("row", "chapter", "topic", "CLL1",
                "Remember", List.of(), "REMEMBER", "Remember"));
        when(assessments.requireVariantEditingContext(COURSE_ID, ASSESSMENT_ID, VARIANT_ID, QUESTION_ID))
                .thenReturn(new VariantEditingContext(assessment, variant, question));
        when(assessments.sanitizeVariantQuestionInput(any(), eq(4)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        return assessments;
    }

    private QuestionRefinementService service(AssessmentService assessments, String response) {
        WebClient client = WebClient.builder().exchangeFunction(request -> reactor.core.publisher.Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(response)
                        .build())).build();
        return new QuestionRefinementService(assessments, client);
    }

    private VariantQuestionInput draft() {
        return new VariantQuestionInput("Question", RichContentFormat.PLAIN_TEXT, null,
                RichContentFormat.PLAIN_TEXT, List.of(
                        new AnswerInput("A", RichContentFormat.PLAIN_TEXT, true),
                        new AnswerInput("B", RichContentFormat.PLAIN_TEXT, false),
                        new AnswerInput("C", RichContentFormat.PLAIN_TEXT, false),
                        new AnswerInput("D", RichContentFormat.PLAIN_TEXT, false)));
    }

    private String response(String changedTarget, String content, String answerB) {
        return """
                {
                  "assistantMessage": "Done",
                  "changedTargets": ["%s"],
                  "draft": {
                    "content": "%s",
                    "contentFormat": "PLAIN_TEXT",
                    "explanation": null,
                    "explanationFormat": "PLAIN_TEXT",
                    "answers": [
                      {"content":"A","contentFormat":"PLAIN_TEXT","correct":true},
                      {"content":"%s","contentFormat":"PLAIN_TEXT","correct":false},
                      {"content":"C","contentFormat":"PLAIN_TEXT","correct":false},
                      {"content":"D","contentFormat":"PLAIN_TEXT","correct":false}
                    ]
                  },
                  "visual": null
                }
                """.formatted(changedTarget, content, answerB);
    }
}
