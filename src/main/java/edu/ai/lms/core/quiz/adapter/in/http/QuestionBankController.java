package edu.ai.lms.core.quiz.adapter.in.http;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.ai.lms.core.quiz.adapter.in.http.request.QuestionBankItemRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.QuestionBankVersionRequest;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuestionBankItemResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuestionBankPageResponse;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelCode;
import edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelOption;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSearchCriteria;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSourceType;
import edu.ai.lms.core.quiz.app.port.in.usecase.QuestionBankUseCase;
import edu.ai.lms.core.quiz.app.service.richcontent.QuestionBankRichContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses/{courseId}")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class QuestionBankController {
    private final QuestionBankUseCase useCase;
    private final QuestionBankRichContentService richContent;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/question-drafts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QuestionBankItemResponse> createDraft(
            @PathVariable UUID courseId,
            @Valid @RequestBody QuestionBankItemRequest request) {
        var result = richContent.createDraft(courseId, request, List.of());
        return ResponseEntity.created(URI.create(
                        "/api/courses/" + courseId + "/question-drafts/" + result.getId()))
                .body(QuestionBankItemResponse.from(result));
    }

    @GetMapping("/question-drafts")
    public ResponseEntity<QuestionBankPageResponse> findDrafts(
            @PathVariable UUID courseId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID chapterId,
            @RequestParam(required = false) UUID learningOutcomeId,
            @RequestParam(required = false) Question.QuestionType questionType,
            @RequestParam(required = false) CognitiveLevelCode cognitiveLevelCode,
            @RequestParam(required = false) QuestionBankSourceType sourceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(QuestionBankPageResponse.fromDrafts(useCase.findDraftPage(criteria(
                courseId, query, chapterId, learningOutcomeId, questionType,
                cognitiveLevelCode, sourceType, page, size))));
    }

    @GetMapping("/question-drafts/{draftId}")
    public ResponseEntity<QuestionBankItemResponse> getDraft(
            @PathVariable UUID courseId,
            @PathVariable UUID draftId) {
        return ResponseEntity.ok(QuestionBankItemResponse.from(useCase.getDraft(courseId, draftId)));
    }

    @PutMapping(value = "/question-drafts/{draftId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QuestionBankItemResponse> updateDraft(
            @PathVariable UUID courseId,
            @PathVariable UUID draftId,
            @RequestParam long version,
            @Valid @RequestBody QuestionBankItemRequest request) {
        var result = richContent.updateDraft(courseId, draftId, version, request, List.of());
        return ResponseEntity.ok(QuestionBankItemResponse.from(result));
    }

    @PutMapping(value = "/question-drafts/{draftId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<QuestionBankItemResponse> updateDraftWithImages(
            @PathVariable UUID courseId,
            @PathVariable UUID draftId,
            @RequestParam long version,
            @RequestPart("payload") String payload,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws Exception {
        QuestionBankItemRequest request = objectMapper.readValue(payload, QuestionBankItemRequest.class);
        var result = richContent.updateDraft(courseId, draftId, version, request, images == null ? List.of() : images);
        return ResponseEntity.ok(QuestionBankItemResponse.from(result));
    }

    @PostMapping("/question-drafts/{draftId}/validate")
    public ResponseEntity<QuestionBankItemResponse> validateDraft(
            @PathVariable UUID courseId,
            @PathVariable UUID draftId,
            @Valid @RequestBody QuestionBankVersionRequest request) {
        return ResponseEntity.ok(QuestionBankItemResponse.from(
                useCase.validateDraft(courseId, draftId, request.version())));
    }

    @DeleteMapping("/question-drafts/{draftId}")
    public ResponseEntity<Void> deleteDraft(
            @PathVariable UUID courseId,
            @PathVariable UUID draftId,
            @RequestParam long version) {
        useCase.deleteDraft(courseId, draftId, version);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/question-bank/questions")
    public ResponseEntity<QuestionBankPageResponse> findBankQuestions(
            @PathVariable UUID courseId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID chapterId,
            @RequestParam(required = false) UUID learningOutcomeId,
            @RequestParam(required = false) Question.QuestionType questionType,
            @RequestParam(required = false) CognitiveLevelCode cognitiveLevelCode,
            @RequestParam(required = false) QuestionBankSourceType sourceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(QuestionBankPageResponse.fromBankQuestions(useCase.findBankPage(criteria(
                courseId, query, chapterId, learningOutcomeId, questionType,
                cognitiveLevelCode, sourceType, page, size))));
    }

    @GetMapping("/question-bank/questions/{questionId}")
    public ResponseEntity<QuestionBankItemResponse> getBankQuestion(
            @PathVariable UUID courseId,
            @PathVariable UUID questionId) {
        return ResponseEntity.ok(QuestionBankItemResponse.from(
                useCase.getBankQuestion(courseId, questionId)));
    }

    @PostMapping("/question-bank/questions/{questionId}/successor-draft")
    public ResponseEntity<QuestionBankItemResponse> createSuccessorDraft(
            @PathVariable UUID courseId,
            @PathVariable UUID questionId) {
        var result = useCase.createSuccessorDraft(courseId, questionId);
        return ResponseEntity.created(URI.create(
                        "/api/courses/" + courseId + "/question-drafts/" + result.getId()))
                .body(QuestionBankItemResponse.from(result));
    }

    @PostMapping(value = "/question-drafts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<QuestionBankItemResponse> createDraftWithImages(
            @PathVariable UUID courseId,
            @RequestPart("payload") String payload,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws Exception {
        QuestionBankItemRequest request = objectMapper.readValue(payload, QuestionBankItemRequest.class);
        var result = richContent.createDraft(courseId, request, images == null ? List.of() : images);
        return ResponseEntity.created(URI.create(
                        "/api/courses/" + courseId + "/question-drafts/" + result.getId()))
                .body(QuestionBankItemResponse.from(result));
    }

    @GetMapping("/question-bank/cognitive-levels")
    public ResponseEntity<List<CognitiveLevelOption>> getCognitiveLevels(@PathVariable UUID courseId) {
        return ResponseEntity.ok(useCase.getCognitiveLevels(courseId));
    }

    private QuestionBankSearchCriteria criteria(
            UUID courseId, String query, UUID chapterId, UUID learningOutcomeId,
            Question.QuestionType questionType, CognitiveLevelCode cognitiveLevelCode,
            QuestionBankSourceType sourceType, int page, int size) {
        return new QuestionBankSearchCriteria(
                courseId, query, chapterId, learningOutcomeId, questionType,
                cognitiveLevelCode, sourceType, page, size);
    }
}
