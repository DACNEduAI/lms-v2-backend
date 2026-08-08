package edu.ai.lms.core.quiz.adapter.in.http;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import edu.ai.lms.core.quiz.adapter.in.http.mapper.GenerateQuizCommandMapper;
import edu.ai.lms.core.quiz.adapter.in.http.request.GenerateQuizRequest;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationJobResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationStatusResponse;
import edu.ai.lms.core.quiz.app.port.in.command.GenerateQuizCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.GenerateQuizUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetQuizGenerationJobUseCase;
import edu.ai.lms.core.quiz.app.service.genquiz.SseQuizGenerationService;
import edu.ai.lms.core.quiz.app.service.genquiz.QuestionVisualGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@Tag(name = "Quiz Generation", description = "Async quiz generation endpoints")
@RequiredArgsConstructor
public class QuizGenerationController {

    private final GenerateQuizUseCase generateQuizUseCase;
    private final GetQuizGenerationJobUseCase getQuizGenerationJobUseCase;
    private final GenerateQuizCommandMapper generateQuizCommandMapper;
    private final SseQuizGenerationService sseQuizGenerationService;
    private final QuestionVisualGenerationService visualGenerationService;

    @Operation(summary = "Start async quiz generation")
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/quizzes/generate")
    public ResponseEntity<QuizGenerationJobResponse> generateQuiz(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody GenerateQuizRequest request) {
        GenerateQuizCommand command = generateQuizCommandMapper.toCommand(request, idempotencyKey);
        QuizGenerationJobResponse response = generateQuizUseCase.generateQuiz(command);
        return ResponseEntity.accepted()
                .header("Location", response.getStatusUrl())
                .body(response);
    }

    @Operation(summary = "Get quiz generation job status")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<QuizGenerationStatusResponse> getJobStatus(@PathVariable UUID jobId) {
        QuizGenerationStatusResponse response = getQuizGenerationJobUseCase.getStatus(jobId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Stream quiz generation job status via SSE")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @GetMapping(value = "/jobs/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamJobStatus(@PathVariable UUID jobId) {
        QuizGenerationStatusResponse status = getQuizGenerationJobUseCase.getStatus(jobId);
        return sseQuizGenerationService.subscribe(jobId, status);
    }

    @Operation(summary = "Retry a failed generated question visual")
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/quizzes/{quizId}/questions/{questionId}/image/retry")
    public ResponseEntity<Void> retryQuestionImage(
            @PathVariable UUID quizId, @PathVariable UUID questionId) {
        visualGenerationService.retryFailed(quizId, questionId);
        return ResponseEntity.accepted().build();
    }
}
