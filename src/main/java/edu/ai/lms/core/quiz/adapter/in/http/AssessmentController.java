package edu.ai.lms.core.quiz.adapter.in.http;

import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.*;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService.AssessmentConfiguration;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService.ContextSelection;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService.DraftVersion;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService.VariantQuestionInput;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses/{courseId}/assessments")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class AssessmentController {
    private final AssessmentService service;

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

    @GetMapping("/{assessmentId}/bank-plan")
    public BankPlan bankPlan(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.bankPlan(courseId, assessmentId);
    }

    @PostMapping("/{assessmentId}/generation-batches")
    public ResponseEntity<BatchView> generate(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return ResponseEntity.accepted().body(service.generateMissing(courseId, assessmentId));
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

    @PostMapping("/{assessmentId}/draft-review/{draftId}/reject")
    public DraftReviewView reject(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID draftId, @RequestBody RejectDraft request) {
        return service.reject(courseId, assessmentId, draftId, request.version(), request.reason());
    }

    @PostMapping("/{assessmentId}/draft-review/{draftId}/validate")
    public DraftReviewView validate(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID draftId, @RequestBody SourceVersion request) {
        return service.validate(courseId, assessmentId, draftId, request.version());
    }

    @PostMapping("/{assessmentId}/draft-review/batch-validate")
    public DraftReviewView validateBatch(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @RequestBody BatchValidate request) {
        return service.validateBatch(courseId, assessmentId, request.drafts());
    }

    @PostMapping("/{assessmentId}/variants")
    public ResponseEntity<List<VariantView>> createVariants(@PathVariable UUID courseId,
            @PathVariable UUID assessmentId) {
        return ResponseEntity.status(201).body(service.createVariants(courseId, assessmentId));
    }

    @GetMapping("/{assessmentId}/variants")
    public List<VariantView> variants(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.variants(courseId, assessmentId);
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

    @PutMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}")
    public List<VariantView> updateQuestion(@PathVariable UUID courseId, @PathVariable UUID assessmentId,
            @PathVariable UUID variantId, @PathVariable UUID questionId,
            @RequestBody VariantQuestionInput request) {
        return service.updateVariantQuestion(courseId, assessmentId, variantId, questionId, request);
    }

    @PostMapping("/{assessmentId}/publish")
    public PublishResult publish(@PathVariable UUID courseId, @PathVariable UUID assessmentId) {
        return service.publish(courseId, assessmentId);
    }

    @PostMapping("/question-bank/recently-used/reset")
    public ResetRecentResult resetRecentlyUsed(@PathVariable UUID courseId, @RequestBody ResetRecent request) {
        return new ResetRecentResult(service.resetRecentlyUsed(courseId, request.questionIds()));
    }

    public record VersionedConfiguration(long version, AssessmentConfiguration configuration) {
    }
    public record SourceVersion(long version) {
    }
    public record RejectDraft(long version, String reason) {
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
