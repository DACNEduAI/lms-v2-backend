package edu.ai.lms.core.quiz.app.service.assessment;

import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.GeneratedAnswer;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.GeneratedDraft;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssessmentWebhookService {
    private final AssessmentWorkflowRepository workflow;

    public void handle(AssessmentWebhookPayload payload) {
        if (payload == null || payload.batchId() == null) throw new IllegalArgumentException("batchId is required");
        if ("FAILED".equalsIgnoreCase(payload.status())) {
            workflow.failBatch(payload.batchId(), payload.errorMessage() == null ? "AI generation failed" : payload.errorMessage());
            return;
        }
        if (!"COMPLETED".equalsIgnoreCase(payload.status())) {
            throw new IllegalArgumentException("Webhook status must be COMPLETED or FAILED");
        }
        List<GeneratedDraft> drafts = payload.drafts() == null ? List.of() : payload.drafts().stream()
                .map(draft -> new GeneratedDraft(draft.content(), draft.type(), draft.matrixRowId(),
                        draft.customLearningLevelId(), draft.learningOutcomeIds() == null ? List.of() : draft.learningOutcomeIds(),
                        draft.answers() == null ? List.of() : draft.answers().stream()
                                .map(answer -> new GeneratedAnswer(answer.content(), answer.correct(), answer.explanation()))
                                .toList()))
                .toList();
        workflow.persistGeneratedDrafts(payload.batchId(), drafts);
    }

    public record AssessmentWebhookPayload(UUID eventId, UUID batchId, String status,
            List<DraftPayload> drafts, String errorCode, String errorMessage) {
    }

    public record DraftPayload(String content, String type, UUID matrixRowId, UUID customLearningLevelId,
            List<UUID> learningOutcomeIds, List<AnswerPayload> answers) {
    }

    public record AnswerPayload(String content, boolean correct, String explanation) {
    }
}
