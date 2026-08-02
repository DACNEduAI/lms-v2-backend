package edu.ai.lms.core.quiz.adapter.in.http;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.quiz.adapter.in.http.request.QuestionBankItemRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.QuestionBankVersionRequest;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuestionBankItemResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuestionBankPageResponse;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankItemStatus;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSearchCriteria;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSourceType;
import edu.ai.lms.core.quiz.app.port.in.usecase.QuestionBankUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses/{courseId}/question-bank/items")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class QuestionBankController {
    private final QuestionBankUseCase useCase;

    @PostMapping
    public ResponseEntity<QuestionBankItemResponse> create(
            @PathVariable UUID courseId,
            @Valid @RequestBody QuestionBankItemRequest request) {
        var result = useCase.createDraft(courseId, request.toCommand());
        return ResponseEntity.created(URI.create(
                        "/api/courses/" + courseId + "/question-bank/items/" + result.item().getId()))
                .body(QuestionBankItemResponse.from(result.item(), result.duplicateWarning()));
    }

    @GetMapping
    public ResponseEntity<QuestionBankPageResponse> findPage(
            @PathVariable UUID courseId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) QuestionBankItemStatus status,
            @RequestParam(required = false) UUID chapterId,
            @RequestParam(required = false) UUID ccctItemId,
            @RequestParam(required = false) UUID learningOutcomeId,
            @RequestParam(required = false) Question.QuestionType questionType,
            @RequestParam(required = false) QuestionBankSourceType sourceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(QuestionBankPageResponse.from(useCase.findPage(new QuestionBankSearchCriteria(
                courseId, query, status, chapterId, ccctItemId, learningOutcomeId,
                questionType, sourceType, page, size))));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<QuestionBankItemResponse> get(
            @PathVariable UUID courseId,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(QuestionBankItemResponse.from(useCase.get(courseId, itemId)));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<QuestionBankItemResponse> updateDraft(
            @PathVariable UUID courseId,
            @PathVariable UUID itemId,
            @RequestParam long version,
            @Valid @RequestBody QuestionBankItemRequest request) {
        var result = useCase.updateDraft(courseId, itemId, version, request.toCommand());
        return ResponseEntity.ok(QuestionBankItemResponse.from(result.item(), result.duplicateWarning()));
    }

    @PostMapping("/{itemId}/ready")
    public ResponseEntity<QuestionBankItemResponse> markReady(
            @PathVariable UUID courseId,
            @PathVariable UUID itemId,
            @Valid @RequestBody QuestionBankVersionRequest request) {
        return ResponseEntity.ok(QuestionBankItemResponse.from(useCase.markReady(courseId, itemId, request.version())));
    }

    @PostMapping("/{itemId}/archive")
    public ResponseEntity<QuestionBankItemResponse> archive(
            @PathVariable UUID courseId,
            @PathVariable UUID itemId,
            @Valid @RequestBody QuestionBankVersionRequest request) {
        return ResponseEntity.ok(QuestionBankItemResponse.from(useCase.archive(courseId, itemId, request.version())));
    }

    @PostMapping("/{itemId}/duplicate")
    public ResponseEntity<QuestionBankItemResponse> duplicate(
            @PathVariable UUID courseId,
            @PathVariable UUID itemId,
            @Valid @RequestBody QuestionBankVersionRequest request) {
        var result = useCase.duplicate(courseId, itemId, request.version());
        return ResponseEntity.created(URI.create(
                        "/api/courses/" + courseId + "/question-bank/items/" + result.item().getId()))
                .body(QuestionBankItemResponse.from(result.item(), result.duplicateWarning()));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteDraft(
            @PathVariable UUID courseId,
            @PathVariable UUID itemId,
            @RequestParam long version) {
        useCase.deleteDraft(courseId, itemId, version);
        return ResponseEntity.noContent().build();
    }
}
