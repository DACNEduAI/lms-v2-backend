package edu.ai.lms.core.quiz3.adapter.in.http;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.quiz3.adapter.in.http.request.QuizVersionRequest;
import edu.ai.lms.core.quiz3.adapter.in.http.response.QuizWorkflowResponse;
import edu.ai.lms.core.quiz3.app.port.in.usecase.QuizWorkflowUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses/{courseId}/quizzes/{quizId}")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class QuizWorkflowController {
    private final QuizWorkflowUseCase useCase;

    @PostMapping("/publish")
    public ResponseEntity<QuizWorkflowResponse> publish(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId,
            @Valid @RequestBody QuizVersionRequest request) {
        return ResponseEntity.ok(QuizWorkflowResponse.from(useCase.publish(courseId, quizId, request.version())));
    }

}
