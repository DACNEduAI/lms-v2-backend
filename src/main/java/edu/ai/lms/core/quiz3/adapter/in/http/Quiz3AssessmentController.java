package edu.ai.lms.core.quiz3.adapter.in.http;

import edu.ai.lms.core.quiz3.app.domain.*;
import edu.ai.lms.core.quiz3.app.service.*;
import edu.ai.lms.core.quiz3.adapter.out.persistence.*;

import static edu.ai.lms.core.quiz3.app.domain.Quiz3Models.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses/{courseId}/quiz3/assessments")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class Quiz3AssessmentController {
    private final Quiz3AssessmentService service;
    private final Quiz3PdfService pdfs;
    private final Quiz3RichContentService richContent;
    private final ObjectMapper objectMapper;
    private final Quiz3QuestionAiService questionAi;
    private final Quiz3QBank2RegenerationService qbank2Regeneration;

    @GetMapping public List<AssessmentView> list(@PathVariable UUID courseId){return service.list(courseId);}
    @PostMapping public ResponseEntity<AssessmentView> create(@PathVariable UUID courseId,@RequestBody Configuration request){return ResponseEntity.status(201).body(service.create(courseId,request));}
    @GetMapping("/{assessmentId}") public AssessmentView get(@PathVariable UUID courseId,@PathVariable UUID assessmentId){return service.get(courseId,assessmentId);}
    @PatchMapping("/{assessmentId}") public AssessmentView update(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@RequestBody VersionedConfiguration request){return service.update(courseId,assessmentId,request.version(),request.configuration());}
    @GetMapping("/{assessmentId}/context") public ContextView context(@PathVariable UUID courseId,@PathVariable UUID assessmentId){return service.context(courseId,assessmentId);}
    @PutMapping("/{assessmentId}/context") public ContextView context(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@RequestBody ContextSelection request){return service.selectContext(courseId,assessmentId,request);}
    @PostMapping("/{assessmentId}/sources/ccct/confirm") public AssessmentView confirmCcct(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@RequestBody SourceVersion request){return service.confirmCcct(courseId,assessmentId,request.version());}
    @PostMapping("/{assessmentId}/sources/matrix/confirm") public AssessmentView confirmMatrix(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@RequestBody SourceVersion request){return service.confirmMatrix(courseId,assessmentId,request.version());}
    @PostMapping("/{assessmentId}/base-quiz/generation") public ResponseEntity<GenerationView> generate(@PathVariable UUID courseId,@PathVariable UUID assessmentId){return ResponseEntity.accepted().body(service.startGeneration(courseId,assessmentId));}
    @GetMapping("/{assessmentId}/base-quiz/generation") public GenerationView generation(@PathVariable UUID courseId,@PathVariable UUID assessmentId){return service.generation(courseId,assessmentId);}
    @PostMapping("/{assessmentId}/base-quiz/confirm") public List<VariantView> confirmBase(@PathVariable UUID courseId,@PathVariable UUID assessmentId){return service.confirmBase(courseId,assessmentId);}
    @PostMapping("/{assessmentId}/variants/reopen-base") public List<VariantView> reopen(@PathVariable UUID courseId,@PathVariable UUID assessmentId){return service.reopenBase(courseId,assessmentId);}
    @GetMapping("/{assessmentId}/variants") public List<VariantView> variants(@PathVariable UUID courseId,@PathVariable UUID assessmentId){return service.variants(courseId,assessmentId);}
    @PostMapping("/{assessmentId}/variants") public ResponseEntity<List<VariantView>> variants(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@RequestBody CreateVariantsRequest request){return ResponseEntity.status(201).body(service.createVariants(courseId,assessmentId,request.variantCount()));}
    @PostMapping("/{assessmentId}/variants/preview/confirm") public AssessmentView confirmVariantPreview(@PathVariable UUID courseId,@PathVariable UUID assessmentId){return service.confirmVariantPreview(courseId,assessmentId);}
    @PostMapping("/{assessmentId}/variants/{variantId}/coverage/recalculate") public List<VariantView> coverage(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@PathVariable UUID variantId){return service.recalculateCoverage(courseId,assessmentId,variantId);}
    @PutMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}") public List<VariantView> updateQuestion(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@PathVariable UUID variantId,@PathVariable UUID questionId,@RequestBody QuestionUpdate request){return service.updateQuestion(courseId,assessmentId,variantId,questionId,request);}
    @PutMapping(value="/{assessmentId}/variants/{variantId}/questions/{questionId}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<VariantView> updateQuestionWithImages(@PathVariable UUID courseId,@PathVariable UUID assessmentId,
            @PathVariable UUID variantId,@PathVariable UUID questionId,@RequestPart("payload")String payload,
            @RequestPart(value="images",required=false)List<MultipartFile> images)throws Exception{
        return richContent.update(courseId,assessmentId,variantId,questionId,
                objectMapper.readValue(payload,QuestionUpdate.class),images==null?List.of():images);}
    @GetMapping(value="/{assessmentId}/variants/{variantId}/pdf",produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@PathVariable UUID variantId,
            @RequestParam(defaultValue="ONLINE_SUBMISSION") Quiz3PdfRenderer.AnswerMode answerMode){var pdf=pdfs.generate(courseId,assessmentId,variantId,answerMode);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(pdf.filename(),StandardCharsets.UTF_8).build().toString()).contentLength(pdf.bytes().length).body(pdf.bytes());}
    @PostMapping("/{assessmentId}/publish") public PublishResult publish(@PathVariable UUID courseId,@PathVariable UUID assessmentId){return service.publish(courseId,assessmentId);}
    @PostMapping("/{assessmentId}/unpublish") public AssessmentView unpublish(@PathVariable UUID courseId,@PathVariable UUID assessmentId){return service.unpublish(courseId,assessmentId);}
    @PostMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-refine") public ResponseEntity<AiJobView> aiRefine(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@PathVariable UUID variantId,@PathVariable UUID questionId,@RequestBody AiRequest request){var result=questionAi.start(courseId,assessmentId,variantId,questionId,request);return ResponseEntity.status(result.existing()?409:202).body(result.job());}
    @GetMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-jobs/{jobId}") public AiJobView aiJob(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@PathVariable UUID variantId,@PathVariable UUID questionId,@PathVariable UUID jobId){return questionAi.job(courseId,assessmentId,variantId,questionId,jobId);}
    @GetMapping(value="/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-jobs/{jobId}/events",produces=MediaType.TEXT_EVENT_STREAM_VALUE) public org.springframework.web.servlet.mvc.method.annotation.SseEmitter aiEvents(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@PathVariable UUID variantId,@PathVariable UUID questionId,@PathVariable UUID jobId){return questionAi.stream(courseId,assessmentId,variantId,questionId,jobId);}
    @PostMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-jobs/{jobId}/cancel") public AiJobView aiCancel(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@PathVariable UUID variantId,@PathVariable UUID questionId,@PathVariable UUID jobId){return questionAi.cancel(courseId,assessmentId,variantId,questionId,jobId);}
    @GetMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-draft") public AiDraft aiDraft(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@PathVariable UUID variantId,@PathVariable UUID questionId){return questionAi.draft(courseId,assessmentId,variantId,questionId);}
    @PostMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-draft/undo") public AiDraft aiUndo(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@PathVariable UUID variantId,@PathVariable UUID questionId){return questionAi.move(courseId,assessmentId,variantId,questionId,false);}
    @PostMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-draft/redo") public AiDraft aiRedo(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@PathVariable UUID variantId,@PathVariable UUID questionId){return questionAi.move(courseId,assessmentId,variantId,questionId,true);}
    @PostMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/ai-draft/discard") public ResponseEntity<Void> aiDiscard(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@PathVariable UUID variantId,@PathVariable UUID questionId){questionAi.discard(courseId,assessmentId,variantId,questionId);return ResponseEntity.noContent().build();}
    @PostMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/qbank2-regenerate") public ResponseEntity<QBank2RegenerationJobView> qbank2Regenerate(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@PathVariable UUID variantId,@PathVariable UUID questionId,@RequestBody QBank2RegenerateRequest request){var result=qbank2Regeneration.start(courseId,assessmentId,variantId,questionId,request);return ResponseEntity.status(result.existing()?409:202).body(result.job());}
    @GetMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/qbank2-regeneration-jobs/{jobId}") public QBank2RegenerationJobView qbank2RegenerationJob(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@PathVariable UUID variantId,@PathVariable UUID questionId,@PathVariable UUID jobId){return qbank2Regeneration.job(courseId,assessmentId,variantId,questionId,jobId);}
    @GetMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/qbank2-regeneration-draft") public QBank2RegenerationDraft qbank2RegenerationDraft(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@PathVariable UUID variantId,@PathVariable UUID questionId){return qbank2Regeneration.draft(courseId,assessmentId,variantId,questionId);}
    @PostMapping("/{assessmentId}/variants/{variantId}/questions/{questionId}/qbank2-regeneration-draft/discard") public ResponseEntity<Void> qbank2RegenerationDiscard(@PathVariable UUID courseId,@PathVariable UUID assessmentId,@PathVariable UUID variantId,@PathVariable UUID questionId){qbank2Regeneration.discard(courseId,assessmentId,variantId,questionId);return ResponseEntity.noContent().build();}
}
