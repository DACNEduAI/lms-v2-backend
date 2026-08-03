package edu.ai.lms.core.quiz.adapter.in.http;

import java.net.URI;
import java.util.List;
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
import edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelCode;
import edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelOption;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSearchCriteria;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSourceType;
import edu.ai.lms.core.quiz.app.port.in.usecase.QuestionBankUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses/{courseId}")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class QuestionBankController {
    private final QuestionBankUseCase useCase;

    @PostMapping("/question-drafts")
    public ResponseEntity<QuestionBankItemResponse> createDraft(
            @PathVariable UUID courseId,
            @Valid @RequestBody QuestionBankItemRequest request) {
        var result = useCase.createDraft(courseId, request.toCommand());
        return ResponseEntity.created(URI.create(
                        "/api/courses/" + courseId + "/question-drafts/" + result.item().getId()))
                .body(QuestionBankItemResponse.from(result.item(), result.duplicateWarning()));
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

    @PutMapping("/question-drafts/{draftId}")
    public ResponseEntity<QuestionBankItemResponse> updateDraft(
            @PathVariable UUID courseId,
            @PathVariable UUID draftId,
            @RequestParam long version,
            @Valid @RequestBody QuestionBankItemRequest request) {
        var result = useCase.updateDraft(courseId, draftId, version, request.toCommand());
        return ResponseEntity.ok(QuestionBankItemResponse.from(result.item(), result.duplicateWarning()));
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
