package edu.ai.lms.core.quiz.app.service.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.GeneratedDraft;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentWebhookService.AnswerPayload;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentWebhookService.AssessmentWebhookPayload;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentWebhookService.DraftPayload;

@ExtendWith(MockitoExtension.class)
class AssessmentWebhookServiceUT {
    @Mock private AssessmentWorkflowRepository workflow;

    @BeforeEach
    void acceptNewWebhookEvents() {
        when(workflow.claimWebhookEvent(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }

    @Test
    void completedCallbackPreservesCellMappingAndSingleCorrectAnswer() {
        UUID batchId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        UUID outcomeId = UUID.randomUUID();
        AssessmentWebhookService service = new AssessmentWebhookService(workflow);
        List<AnswerPayload> answers = List.of(
                new AnswerPayload("A", true, "Correct"),
                new AnswerPayload("B", false, "Incorrect"),
                new AnswerPayload("C", false, "Incorrect"),
                new AnswerPayload("D", false, "Incorrect"));

        service.handle(new AssessmentWebhookPayload(UUID.randomUUID(), batchId, "COMPLETED",
                List.of(new DraftPayload("Question", "MULTIPLE_CHOICE", rowId, levelId,
                        List.of(outcomeId), answers)), null, null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GeneratedDraft>> captor = ArgumentCaptor.forClass(List.class);
        verify(workflow).persistGeneratedDrafts(org.mockito.ArgumentMatchers.eq(batchId), captor.capture());
        GeneratedDraft draft = captor.getValue().getFirst();
        assertEquals(rowId, draft.matrixRowId());
        assertEquals(levelId, draft.customLearningLevelId());
        assertEquals(1, draft.answers().stream().filter(answer -> answer.correct()).count());
    }

    @Test
    void failedCallbackRecordsFailureWithoutCreatingDrafts() {
        UUID batchId = UUID.randomUUID();
        AssessmentWebhookService service = new AssessmentWebhookService(workflow);
        service.handle(new AssessmentWebhookPayload(UUID.randomUUID(), batchId, "FAILED",
                List.of(), "GENERATION", "cell failed"));
        verify(workflow).failBatch(batchId, "cell failed");
        verify(workflow, never()).persistGeneratedDrafts(org.mockito.ArgumentMatchers.any(), anyList());
    }

    @Test
    void rejectsNonTerminalWebhookStatus() {
        AssessmentWebhookService service = new AssessmentWebhookService(workflow);
        assertThrows(IllegalArgumentException.class, () -> service.handle(
                new AssessmentWebhookPayload(UUID.randomUUID(), UUID.randomUUID(), "PROCESSING",
                        List.of(), null, null)));
    }
}
