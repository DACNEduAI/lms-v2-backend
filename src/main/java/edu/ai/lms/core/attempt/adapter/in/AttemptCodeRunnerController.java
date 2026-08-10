package edu.ai.lms.core.attempt.adapter.in;

import edu.ai.lms.core.attempt.adapter.in.request.ExecuteCodeRequest;
import edu.ai.lms.core.attempt.app.domain.AttemptCodeRunnerQuestion;
import edu.ai.lms.core.attempt.app.domain.AttemptCodeSubmission;
import edu.ai.lms.core.attempt.app.service.AttemptCodeRunnerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/{courseId}/attempt/{attemptId}/questions/{questionId}/code-runner")
@PreAuthorize("hasRole('STUDENT')")
public class AttemptCodeRunnerController {
    private final AttemptCodeRunnerService service;

    @GetMapping
    public AttemptCodeRunnerQuestion getQuestion(@PathVariable UUID courseId, @PathVariable UUID attemptId,
                                                  @PathVariable UUID questionId) {
        return service.getQuestion(courseId, attemptId, questionId);
    }

    @PostMapping("/runs")
    @ResponseStatus(HttpStatus.CREATED)
    public AttemptCodeSubmission run(@PathVariable UUID courseId, @PathVariable UUID attemptId,
                                      @PathVariable UUID questionId,
                                      @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
                                      @Valid @RequestBody ExecuteCodeRequest request) {
        return service.run(courseId, attemptId, questionId, request.sourceCode(), idempotencyKey, "RUN");
    }

    @PostMapping("/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public AttemptCodeSubmission submit(@PathVariable UUID courseId, @PathVariable UUID attemptId,
                                         @PathVariable UUID questionId,
                                         @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
                                         @Valid @RequestBody ExecuteCodeRequest request) {
        return service.run(courseId, attemptId, questionId, request.sourceCode(), idempotencyKey, "SUBMIT");
    }
}
