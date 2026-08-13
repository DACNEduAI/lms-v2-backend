package edu.ai.lms.core.quiz.app.service.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository.AnswerInput;
import edu.ai.lms.core.quiz.adapter.out.persistence.QuestionAiRepository;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.app.domain.assessment.QuestionAiModels.*;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentAsset;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService.VariantEditingContext;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService.VariantQuestionInput;
import edu.ai.lms.core.quiz.app.service.richcontent.RichContentUploadService;
import edu.ai.lms.core.quiz.app.service.richcontent.RichContentPolicyService;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichTextValue;
import edu.ai.lms.core.quiz.adapter.out.persistence.RichContentAssetRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionAiService {
    private final AssessmentService assessments;
    private final QuestionAiRepository repository;
    private final QuestionAiJobProcessor processor;
    private final QuestionAiSseService events;
    private final LoadSignedInUserPort signedInUserPort;
    private final ObjectMapper objectMapper;
    private final RichContentUploadService uploads;
    private final RichContentAssetRepository richAssets;
    private final RichContentPolicyService richContentPolicy;
    @Qualifier("questionAiExecutor")
    private final ThreadPoolTaskExecutor questionAiExecutor;

    @Transactional
    public JobView start(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId, StartJobRequest request) {
        if (request == null || request.mode() == null) throw new IllegalArgumentException("AI job mode is required");
        String prompt = request.prompt() == null ? "" : request.prompt().trim();
        if ((request.mode() == Mode.ORCHESTRATE || request.mode() == Mode.TEXT) && prompt.isBlank()) {
            throw new IllegalArgumentException("This AI action requires a prompt");
        }
        if (prompt.length() > 4000) throw new IllegalArgumentException("AI prompt is too long");
        VariantEditingContext context = assessments.requireVariantEditingContext(courseId, assessmentId, variantId, questionId);
        long expected = request.expectedQuestionVersion() == null ? context.question().version() : request.expectedQuestionVersion();
        if (expected != context.question().version()) throw new IllegalStateException("Question version changed; reload before starting AI");
        CurrentActor actor = signedInUserPort.getAuthenticatedUser().orElseThrow();
        var draft = repository.ensureDraft(assessmentId, variantId, questionId, expected);
        if (request.currentContent() != null) {
            RichTextValue current = richContentPolicy.sanitize(new RichTextValue(request.currentContent(),
                    request.currentContentFormat() == null ? draft.contentFormat() : request.currentContentFormat()),
                    RichContentPolicyService.MAX_PROMPT_HTML_LENGTH);
            repository.syncDraftBaseline(questionId, current.content(), current.contentFormat());
            draft = repository.draft(questionId).orElseThrow();
        }
        JsonNode payload = objectMapper.valueToTree(request);
        UUID jobId = repository.createJob(assessmentId, variantId, questionId, actor.id(), request.mode(), prompt,
                payload, draft.baseQuestionVersion());
        return repository.jobView(jobId);
    }

    public JobView job(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId, UUID jobId) {
        authorize(courseId, assessmentId, variantId, questionId);
        JobView value = repository.jobView(jobId);
        if (!value.questionId().equals(questionId)) throw new IllegalArgumentException("AI job does not belong to this question");
        return value;
    }

    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter stream(
            UUID courseId, UUID assessmentId, UUID variantId, UUID questionId, UUID jobId) {
        return events.subscribe(jobId, job(courseId, assessmentId, variantId, questionId, jobId));
    }

    public JobView clarify(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId, UUID jobId,
            ClarificationRequest request) {
        job(courseId, assessmentId, variantId, questionId, jobId);
        if (request == null || request.answer() == null || request.answer().isBlank() || request.answer().length() > 4000) {
            throw new IllegalArgumentException("A clarification answer is required");
        }
        if (!repository.answerClarification(jobId, request.answer().trim())) {
            throw new IllegalStateException("AI clarification has expired or the job is no longer waiting");
        }
        return repository.jobView(jobId);
    }

    public JobView cancel(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId, UUID jobId) {
        job(courseId, assessmentId, variantId, questionId, jobId);
        if (!repository.cancel(jobId)) throw new IllegalStateException("AI job is already complete");
        JobView value = repository.jobView(jobId);
        events.push(value);
        return value;
    }

    public WorkingDraftView draft(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId) {
        authorize(courseId, assessmentId, variantId, questionId);
        return repository.draftView(questionId);
    }

    public TimelineView timeline(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId) {
        authorize(courseId, assessmentId, variantId, questionId);
        return new TimelineView(repository.timeline(questionId));
    }

    @Transactional
    public JobView render(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId,
            StructuredRenderRequest request) {
        if (request == null || request.artifactId() == null || request.spec() == null || request.spec().isNull()) {
            throw new IllegalArgumentException("Structured SVG artifact and spec are required");
        }
        VariantEditingContext context = authorize(courseId, assessmentId, variantId, questionId);
        ArtifactView artifact = repository.artifact(questionId, request.artifactId())
                .orElseThrow(() -> new IllegalArgumentException("AI SVG was not found"));
        if (artifact.type() != ArtifactType.SVG || !"SPECIALIZED".equals(artifact.generator())) {
            throw new IllegalArgumentException("Only a structured SVG can be rendered from data/layout");
        }
        long expected = request.expectedQuestionVersion() == null ? context.question().version() : request.expectedQuestionVersion();
        if (expected != context.question().version()) throw new IllegalStateException("Question version changed; reload first");
        CurrentActor actor = signedInUserPort.getAuthenticatedUser().orElseThrow();
        var draft = repository.ensureDraft(assessmentId, variantId, questionId, expected);
        var payload = objectMapper.createObjectNode().put("operation", "EDIT")
                .put("artifactId", request.artifactId().toString()).put("altText", request.altText());
        payload.set("renderSpec", request.spec());
        if (request.layoutOverride() != null && !request.layoutOverride().isNull()) {
            payload.set("layoutOverride", request.layoutOverride());
        }
        UUID jobId = repository.createJob(assessmentId, variantId, questionId, actor.id(), Mode.SVG,
                "Render structured SVG data/layout", payload, draft.baseQuestionVersion());
        return repository.jobView(jobId);
    }

    public WorkingDraftView undo(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId) {
        authorize(courseId, assessmentId, variantId, questionId);
        requireIdle(questionId);
        return repository.moveChangePointer(questionId, false);
    }

    public WorkingDraftView redo(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId) {
        authorize(courseId, assessmentId, variantId, questionId);
        requireIdle(questionId);
        return repository.moveChangePointer(questionId, true);
    }

    @Transactional
    public List<edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantView> save(
            UUID courseId, UUID assessmentId, UUID variantId, UUID questionId, long expectedQuestionVersion,
            VariantQuestionInput teacherDraft) {
        VariantEditingContext context = authorize(courseId, assessmentId, variantId, questionId);
        requireIdle(questionId);
        WorkingDraftView draft = repository.draftView(questionId);
        if (expectedQuestionVersion != draft.baseQuestionVersion() || context.question().version() != expectedQuestionVersion) {
            throw new IllegalStateException("Question version changed; reload before saving the AI draft");
        }
        Document document = Jsoup.parseBodyFragment(draft.content());
        List<RichContentUploadService.UploadedAsset> uploaded = new ArrayList<>();
        for (Element image : document.body().select("img[data-ai-artifact-type=SVG][data-ai-artifact-id]")) {
            UUID artifactId = UUID.fromString(image.attr("data-ai-artifact-id"));
            ArtifactView artifact = repository.artifact(questionId, artifactId)
                    .orElseThrow(() -> new IllegalArgumentException("AI SVG does not belong to this question"));
            RichContentUploadService.UploadedAsset stored = uploads.uploadAiSvg(artifact.sourceCode());
            uploaded.add(stored);
            image.attr("src", stored.publicUrl());
            richAssets.save(new RichContentAsset(UUID.randomUUID(), null, null, questionId,
                    stored.storageKey(), stored.mediaType(), stored.byteSize(), stored.sha256(), artifactId, null, null));
            repository.markArtifactStored(artifactId, stored.storageKey());
        }
        uploads.cleanupAfterRollback(uploaded);
        VariantQuestionInput submitted = teacherDraft == null
                ? new VariantQuestionInput(context.question().content(), context.question().contentFormat(),
                    context.question().explanation(), context.question().explanationFormat(),
                    context.question().answers().stream().map(answer -> new AnswerInput(answer.content(),
                            answer.contentFormat(), answer.correct())).toList())
                : assessments.sanitizeVariantQuestionInput(teacherDraft, context.assessment().optionCount());
        List<AnswerInput> answers = submitted.answers();
        VariantQuestionInput update = new VariantQuestionInput(document.body().html(), draft.contentFormat(),
                submitted.explanation(), submitted.explanationFormat(), answers);
        var result = assessments.updateVariantQuestion(courseId, assessmentId, variantId, questionId,
                expectedQuestionVersion, update);
        repository.discardDraft(questionId);
        return result;
    }

    @Transactional
    public void discard(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId) {
        authorize(courseId, assessmentId, variantId, questionId);
        requireIdle(questionId);
        repository.discardDraft(questionId);
    }

    public ArtifactView artifact(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId, UUID artifactId) {
        authorize(courseId, assessmentId, variantId, questionId);
        return repository.artifact(questionId, artifactId)
                .orElseThrow(() -> new IllegalArgumentException("AI artifact was not found"));
    }

    public List<ArtifactView> artifacts(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId) {
        authorize(courseId, assessmentId, variantId, questionId);
        String content = assessments.requireVariantEditingContext(courseId, assessmentId, variantId, questionId)
                .question().content();
        return repository.artifacts(questionId).stream()
                .filter(artifact -> content.contains(artifact.id().toString()))
                .toList();
    }

    @Scheduled(fixedDelayString = "${quiz.question-ai.dispatch-delay-ms:1000}")
    public void dispatchQueued() {
        repository.expireJobs();
        int workerSlots = Math.max(0, questionAiExecutor.getMaxPoolSize() - questionAiExecutor.getActiveCount());
        int available = Math.min(20, workerSlots);
        if (available == 0) return;
        repository.queuedJobs(available).forEach(jobId -> {
            if (repository.claim(jobId)) processor.processClaimedAsync(jobId);
        });
    }

    private VariantEditingContext authorize(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId) {
        return assessments.requireVariantEditingContext(courseId, assessmentId, variantId, questionId);
    }

    private void requireIdle(UUID questionId) {
        if (repository.hasActiveJob(questionId)) throw new IllegalStateException("Wait for or cancel the active AI job first");
    }
}
