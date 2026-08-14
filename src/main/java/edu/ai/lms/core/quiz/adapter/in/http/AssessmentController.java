package edu.ai.lms.core.quiz.adapter.in.http;

import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.*;

import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService;
import edu.ai.lms.core.quiz.app.service.assessment.VariantPdfService;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService.AssessmentConfiguration;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService.ContextSelection;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService.DraftVersion;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService.VariantQuestionInput;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService.VariantQuestionUpdate;
import edu.ai.lms.core.quiz.app.service.assessment.QuestionRefinementService;
import edu.ai.lms.core.quiz.app.service.assessment.QuestionRefinementService.RefinementRequest;
import edu.ai.lms.core.quiz.app.service.assessment.QuestionRefinementService.RefinementResponse;
import edu.ai.lms.core.quiz.app.service.assessment.QuestionAiService;
import edu.ai.lms.core.quiz.app.domain.assessment.QuestionAiModels.*;
import edu.ai.lms.core.quiz.app.service.richcontent.AssessmentRichContentService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses/{courseId}/assessments")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AssessmentController {
    private final AssessmentService service;
    private final VariantPdfService variantPdfs;
    private final AssessmentRichContentService richContent;
    private final QuestionRefinementService refinements;
    private final QuestionAiService questionAi;
    private final ObjectMapper objectMapper;

    public AssessmentController(AssessmentService service, VariantPdfService variantPdfs,
            AssessmentRichContentService richContent, QuestionRefinementService refinements,
            ObjectMapper objectMapper) {
        this(service, variantPdfs, richContent, refinements, null, objectMapper);
    }

    @GetMapping
    public List<AssessmentView> list(@PathVariable UUID courseId) {
        return service.list(courseId);
    }

    @PostMapping
    public ResponseEntity<AssessmentView> create(@PathVariable UUID courseId,
            @RequestBody AssessmentConfiguration request) {
        return ResponseEntity.status(201).body(service.create(courseId, request));
    }

    @GetMapping("/{assessmentId}")
    public AssessmentView get(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.get(courseId, assessmentId);
    }

    @PatchMapping("/{assessmentId}")
    public AssessmentView update(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @RequestBody VersionedConfiguration request) {
        return service.update(courseId, assessmentId, request.version(), request.configuration());
    }

    @GetMapping("/{assessmentId}/context")
    public ContextView context(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.context(courseId, assessmentId);
    }

    @PutMapping("/{assessmentId}/context")
    public ContextView selectContext(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @RequestBody ContextSelection request) {
        return service.selectContext(courseId, assessmentId, request);
    }

    @PostMapping("/{assessmentId}/sources/ccct/confirm")
    public AssessmentView confirmCcct(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @RequestBody SourceVersion request) {
        return service.confirmCcct(courseId, assessmentId, request.version());
    }

    @PostMapping("/{assessmentId}/sources/matrix/confirm")
    public AssessmentView confirmMatrix(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @RequestBody SourceVersion request) {
        return service.confirmMatrix(courseId, assessmentId, request.version());
    }

    @PostMapping("/{assessmentId}/sources/rollback")
    public AssessmentView rollbackSources(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.rollbackSources(courseId, assessmentId);
    }

    @PostMapping("/{assessmentId}/base-quiz/generation")
    public ResponseEntity<BaseGenerationView> startBaseGeneration(@PathVariable UUID courseId,
            @PathVariable UUID assessmentId, @RequestBody BaseGenerationRequest request) {
        return ResponseEntity.accepted()
                .body(service.startBaseGeneration(courseId, assessmentId, request.optionCount()));
    }

    @GetMapping("/{assessmentId}/base-quiz/generation")
    public BaseGenerationView baseGeneration(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.reconcileBaseGeneration(courseId, assessmentId);
    }

    @PostMapping("/{assessmentId}/base-quiz/confirm")
    public List<VariantView> confirmBaseQuiz(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.confirmBaseQuiz(courseId, assessmentId);
    }

    @GetMapping("/{assessmentId}/bank-plan")
    public BankPlan bankPlan(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.bankPlan(courseId, assessmentId);
    }

    @PostMapping("/{assessmentId}/generation-batches")
    public ResponseEntity<BatchView> generate(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return ResponseEntity.accepted().body(service.generateMissing(courseId, assessmentId));
    }

    @GetMapping("/{assessmentId}/generation-batches")
    public List<BatchView> batches(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.batches(courseId, assessmentId);
    }

    @GetMapping("/{assessmentId}/generation-batches/{batchId}")
    public BatchView batch(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID batchId) {
        return service.getBatch(courseId, assessmentId, batchId);
    }

    @PostMapping("/{assessmentId}/generation-batches/{batchId}/cells/{cellId}/retry")
    public ResponseEntity<BatchView> retryCell(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID batchId, @PathVariable UUID cellId) {
        return ResponseEntity.accepted().body(service.retryCell(courseId, assessmentId, batchId, cellId));
    }

    @GetMapping("/{assessmentId}/draft-review")
    public DraftReviewView review(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.review(courseId, assessmentId);
    }

    @GetMapping("/{assessmentId}/draft-review/items")
    public DraftReviewPage reviewItems(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID chapterId,
            @RequestParam(required = false) UUID learningOutcomeId,
            @RequestParam(required = false) String cognitiveLevelCode,
            @RequestParam(required = false) UUID generationBatchId,
            @RequestParam(required = false) String questionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return service.reviewPage(courseId, assessmentId, new DraftReviewSearch(query, status, chapterId,
                learningOutcomeId, cognitiveLevelCode, generationBatchId, questionType, page, size));
    }

    @GetMapping("/{assessmentId}/draft-review/criteria")
    public DraftReviewCriteria reviewCriteria(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.reviewCriteria(courseId, assessmentId);
    }

    @PostMapping("/{assessmentId}/draft-review/{draftId}/reject")
    public DraftReviewView reject(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID draftId, @RequestBody RejectDraft request) {
        return service.reject(courseId, assessmentId, draftId, request.version(), request.reason());
    }

    @PostMapping("/{assessmentId}/draft-review/{draftId}/validate")
    public DraftValidationResult validate(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID draftId, @RequestBody SourceVersion request) {
        return service.validate(courseId, assessmentId, draftId, request.version());
    }

    @PostMapping("/{assessmentId}/draft-review/{draftId}/regenerate")
    public ResponseEntity<BatchView> regenerateDraft(@PathVariable UUID courseId,
            @PathVariable UUID assessmentId, @PathVariable UUID draftId,
            @RequestBody SourceVersion request) {
        return ResponseEntity.accepted()
                .body(service.regenerateDraft(courseId, assessmentId, draftId, request.version()));
    }

    @PostMapping("/{assessmentId}/draft-review/batch-validate")
    public BatchValidationResult validateBatch(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @RequestBody BatchValidate request) {
        return service.validateBatch(courseId, assessmentId, request.drafts());
    }

    @PostMapping("/{assessmentId}/variants")
    public ResponseEntity<List<VariantView>> createVariants(@PathVariable UUID courseId,
            @PathVariable UUID assessmentId, @RequestBody CreateVariantsRequest request) {
        return ResponseEntity.status(201).body(service.createVariants(courseId, assessmentId, request.variantCount()));
    }

    @PostMapping("/{assessmentId}/variants/preview/confirm")
    public AssessmentView confirmVariantPreview(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.confirmVariantPreview(courseId, assessmentId);
    }

    @PostMapping("/{assessmentId}/variants/reopen-base")
    public List<VariantView> reopenBaseQuiz(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.reopenBaseQuiz(courseId, assessmentId);
    }

    @GetMapping("/{assessmentId}/variants")
    public List<VariantView> variants(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.variants(courseId, assessmentId);
    }

    @GetMapping(value = "/{assessmentId}/variants/{variantId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadVariantPdf(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId) {
        var pdf = variantPdfs.generate(courseId, assessmentId, variantId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(pdf.filename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.bytes().length)
                .body(pdf.bytes());
    }

    @PostMapping("/{assessmentId}/variants/{variantId}/coverage/recalculate")
    public List<VariantView> recalculateCoverage(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId) {
        return service.recalculateCoverage(courseId, assessmentId, variantId);
    }

    @PostMapping("/{assessmentId}/variants/{variantId}/coverage/override")
    public List<VariantView> overrideCoverage(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @RequestBody OverrideCoverage request) {
        return service.overrideCoverage(courseId, assessmentId, variantId, request.reason());
    }

    @PutMapping(value = "/{assessmentId}/variants/{variantId}/questions/{questionId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<VariantView> updateQuestion(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId,
            @RequestBody VariantQuestionUpdate request) {
        return richContent.updateVariantQuestion(courseId, assessmentId, variantId, questionId,
                request.expectedQuestionVersion(), request.draft(), List.of());
    }

    @PutMapping(value = "/{assessmentId}/variants/{variantId}/questions/{questionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<VariantView> updateQuestionWithImages(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId,
            @RequestPart("payload") String payload,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws Exception {
        VariantQuestionUpdate request = objectMapper.readValue(payload, VariantQuestionUpdate.class);
        return richContent.updateVariantQuestion(courseId, assessmentId, variantId, questionId,
                request.expectedQuestionVersion(), request.draft(), images == null ? List.of() : images);
    }

    @PostMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-refine")
    public RefinementResponse refineQuestion(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId,
            @RequestBody RefinementRequest request) {
        return refinements.refine(courseId, assessmentId, variantId, questionId, request);
    }

    @PostMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-jobs")
    public ResponseEntity<JobView> startQuestionAiJob(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId, @RequestBody StartJobRequest request) {
        return ResponseEntity.accepted().body(questionAi.start(courseId, assessmentId, variantId, questionId, request));
    }

    @GetMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-jobs/{jobId}")
    public JobView questionAiJob(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId, @PathVariable UUID jobId) {
        return questionAi.job(courseId, assessmentId, variantId, questionId, jobId);
    }

    @GetMapping(value = "/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-jobs/{jobId}/events",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamQuestionAiJob(
            @PathVariable UUID courseId, @PathVariable UUID assessmentId, @PathVariable UUID variantId,
            @PathVariable UUID questionId, @PathVariable UUID jobId) {
        return questionAi.stream(courseId, assessmentId, variantId, questionId, jobId);
    }

    @PostMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-jobs/{jobId}/clarification")
    public JobView clarifyQuestionAiJob(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId, @PathVariable UUID jobId,
            @RequestBody ClarificationRequest request) {
        return questionAi.clarify(courseId, assessmentId, variantId, questionId, jobId, request);
    }

    @PostMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-jobs/{jobId}/cancel")
    public JobView cancelQuestionAiJob(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId, @PathVariable UUID jobId) {
        return questionAi.cancel(courseId, assessmentId, variantId, questionId, jobId);
    }

    @GetMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-draft")
    public WorkingDraftView questionAiDraft(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId) {
        return questionAi.draft(courseId, assessmentId, variantId, questionId);
    }

    @GetMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-timeline")
    public TimelineView questionAiTimeline(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId) {
        return questionAi.timeline(courseId, assessmentId, variantId, questionId);
    }

    @PostMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-draft/undo")
    public WorkingDraftView undoQuestionAiDraft(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId) {
        return questionAi.undo(courseId, assessmentId, variantId, questionId);
    }

    @PostMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-draft/redo")
    public WorkingDraftView redoQuestionAiDraft(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId) {
        return questionAi.redo(courseId, assessmentId, variantId, questionId);
    }

    @PostMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-draft/render-svg")
    public ResponseEntity<JobView> renderQuestionAiSvg(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId,
            @RequestBody StructuredRenderRequest request) {
        return ResponseEntity.accepted().body(questionAi.render(courseId, assessmentId, variantId, questionId, request));
    }

    @PostMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-draft/save")
    public List<VariantView> saveQuestionAiDraft(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId,
            @RequestBody SaveAiDraftRequest request) {
        return questionAi.save(courseId, assessmentId, variantId, questionId,
                request.expectedQuestionVersion(), request.draft());
    }

    @DeleteMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-draft")
    public ResponseEntity<Void> discardQuestionAiDraft(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId) {
        questionAi.discard(courseId, assessmentId, variantId, questionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-artifacts/{artifactId}/content",
            produces = "image/svg+xml")
    public ResponseEntity<String> questionAiArtifactContent(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId, @PathVariable UUID artifactId) {
        ArtifactView artifact = questionAi.artifact(courseId, assessmentId, variantId, questionId, artifactId);
        if (artifact.type() != ArtifactType.SVG) throw new IllegalArgumentException("Artifact is not an SVG");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.valueOf("image/svg+xml")).body(artifact.sourceCode());
    }

    @GetMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-artifacts")
    public List<ArtifactView> questionAiArtifacts(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId) {
        return questionAi.artifacts(courseId, assessmentId, variantId, questionId);
    }

    @PostMapping("/{assessmentId}/publish")
    public PublishResult publish(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.publish(courseId, assessmentId);
    }

    @PostMapping("/{assessmentId}/unpublish")
    public AssessmentView unpublish(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.unpublish(courseId, assessmentId);
    }

    @PostMapping("/question-bank/recently-used/reset")
    public ResetRecentResult resetRecentlyUsed(@PathVariable UUID courseId, @RequestBody ResetRecent request) {
        return new ResetRecentResult(service.resetRecentlyUsed(courseId, request.questionIds()));
    }

    public record VersionedConfiguration(long version, AssessmentConfiguration configuration) {
    }
    public record SourceVersion(long version) {
    }
    public record BaseGenerationRequest(int optionCount) {
    }
    public record CreateVariantsRequest(int variantCount) {
    }
    public record RejectDraft(long version, String reason) {
    }
    public record SaveAiDraftRequest(long expectedQuestionVersion, VariantQuestionInput draft) {
    }
    public record BatchValidate(List<DraftVersion> drafts) {
    }
    public record OverrideCoverage(String reason) {
    }
    public record ResetRecent(List<UUID> questionIds) {
    }
    public record ResetRecentResult(int resetCount) {
    }
}
