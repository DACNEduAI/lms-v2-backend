package edu.ai.lms.core.quiz.app.service.assessment;

import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.GeneratedAnswer;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.GeneratedDraft;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentWebhookService {
    private final AssessmentWorkflowRepository workflow;

    public void handle(AssessmentWebhookPayload payload) {
        if (payload == null || payload.batchId() == null) throw new IllegalArgumentException("batchId is required");
        int draftCount = payload.drafts() == null ? 0 : payload.drafts().size();
        log.info("event=assessment.generation.callback_received event_id={} batch_id={} status={} draft_count={}",
                payload.eventId(), payload.batchId(), payload.status(), draftCount);
        if ("FAILED".equalsIgnoreCase(payload.status())) {
            log.warn("event=assessment.generation.callback_failed batch_id={} error_code={}",
                    payload.batchId(), payload.errorCode());
            workflow.failBatch(payload.batchId(), payload.errorMessage() == null ? "AI generation failed" : payload.errorMessage());
            return;
        }
        if (!"COMPLETED".equalsIgnoreCase(payload.status())) {
            throw new IllegalArgumentException("Webhook status must be COMPLETED or FAILED");
        }
        List<GeneratedDraft> drafts = payload.drafts() == null ? List.of() : payload.drafts().stream()
                .map(draft -> new GeneratedDraft(draft.content(), format(draft.contentFormat()), draft.type(), draft.matrixRowId(),
                        draft.customLearningLevelId(), draft.learningOutcomeIds() == null ? List.of() : draft.learningOutcomeIds(),
                        draft.answers() == null ? List.of() : draft.answers().stream()
                                .map(answer -> new GeneratedAnswer(answer.content(), format(answer.contentFormat()), answer.correct(),
                                        answer.explanation(), format(answer.explanationFormat())))
                                .toList()))
                .toList();
        workflow.persistGeneratedDrafts(payload.batchId(), drafts);
        log.info("event=assessment.generation.callback_persisted batch_id={} draft_count={}",
                payload.batchId(), drafts.size());
    }

    public record AssessmentWebhookPayload(UUID eventId, UUID batchId, String status,
            List<DraftPayload> drafts, String errorCode, String errorMessage) {
    }

    public record DraftPayload(String content, RichContentFormat contentFormat, String type, UUID matrixRowId, UUID customLearningLevelId,
            List<UUID> learningOutcomeIds, List<AnswerPayload> answers) {
        public DraftPayload(String content, String type, UUID matrixRowId, UUID customLearningLevelId,
                List<UUID> learningOutcomeIds, List<AnswerPayload> answers) {
            this(content, RichContentFormat.PLAIN_TEXT, type, matrixRowId, customLearningLevelId, learningOutcomeIds, answers);
        }
    }

    public record AnswerPayload(String content, RichContentFormat contentFormat, boolean correct,
            String explanation, RichContentFormat explanationFormat) {
        public AnswerPayload(String content, boolean correct, String explanation) {
            this(content, RichContentFormat.PLAIN_TEXT, correct, explanation, RichContentFormat.PLAIN_TEXT);
        }
    }

    private static RichContentFormat format(RichContentFormat value) {
        return value == null ? RichContentFormat.PLAIN_TEXT : value;
    }
}
