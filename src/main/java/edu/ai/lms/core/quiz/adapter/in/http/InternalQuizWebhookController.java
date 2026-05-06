package edu.ai.lms.core.quiz.adapter.in.http;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.quiz.adapter.in.http.request.QuizWebhookRequest;
import edu.ai.lms.core.quiz.app.service.QuizWebhookProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/internal/webhooks")
@Tag(name = "Quiz Generation Webhook", description = "Internal callbacks for quiz generation")
@RequiredArgsConstructor
public class InternalQuizWebhookController {

    private static final String WEBHOOK_HEADER = "X-Quiz-Webhook-Token";

    private final QuizWebhookProcessingService quizWebhookProcessingService;

    @Value("${quiz.generation.webhook.secret}")
    private String webhookSecret;

    @Operation(summary = "Receive quiz generation webhook")
    @PostMapping("/quiz-ready")
    public ResponseEntity<Void> handleQuizWebhook(
            @RequestHeader(value = WEBHOOK_HEADER, required = false) String token,
            @RequestBody QuizWebhookRequest request) {
        if (!Objects.equals(webhookSecret, token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        quizWebhookProcessingService.handleWebhook(request);
        return ResponseEntity.ok().build();
    }
}
