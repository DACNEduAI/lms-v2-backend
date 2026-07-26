package edu.ai.lms.core.quiz.adapter.in.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.quiz.adapter.in.http.request.QuizWebhookRequest;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationStatusResponse;
import edu.ai.lms.core.quiz.app.service.QuizWebhookProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/internal/webhooks")
@RequiredArgsConstructor
public class InternalQuizWebhookController {
    private final QuizWebhookProcessingService processingService;
    private final ObjectMapper objectMapper;

    @Value("${quiz.generation.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/quiz-generation")
    public ResponseEntity<QuizGenerationStatusResponse> handle(
            @RequestHeader(value = "X-Webhook-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestBody String rawBody) throws Exception {
        if (!isValid(timestamp, signature, rawBody)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        QuizWebhookRequest request = objectMapper.readValue(rawBody, QuizWebhookRequest.class);
        return ResponseEntity.ok(processingService.handleWebhook(request));
    }

    private boolean isValid(String timestamp, String signature, String rawBody) {
        if (timestamp == null || signature == null) return false;
        try {
            long sentAt = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - sentAt) > 300) return false;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + rawBody)
                    .getBytes(StandardCharsets.UTF_8)));
            String supplied = signature.startsWith("v1=") ? signature.substring(3) : signature;
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    supplied.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return false;
        }
    }
}
