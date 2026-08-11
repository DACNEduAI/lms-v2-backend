package edu.ai.lms.core.quiz.app.service.assessment;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssessmentGenerationDispatcher {
    private final AssessmentWorkflowRepository workflow;
    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper;

    @Value("${quiz.generation.webhook.base-url}")
    private String webhookBaseUrl;
    @Value("${quiz.generation.webhook.secret}")
    private String webhookSecret;
    @Value("${quiz.generation.timeout-minutes:20}")
    private long timeoutMinutes;

    @Async
    public void dispatch(UUID batchId) {
        workflow.markBatchProcessing(batchId);
        try {
            send(batchId, workflow.batchRequestSnapshot(batchId));
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            workflow.failBatch(batchId, message.substring(0, Math.min(1000, message.length())));
        }
    }

    @Async
    public void retryCell(UUID batchId, String requestSnapshot) {
        try {
            send(batchId, requestSnapshot);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            workflow.failBatch(batchId, message.substring(0, Math.min(1000, message.length())));
        }
    }

    private void send(UUID batchId, String requestSnapshot) throws Exception {
        ObjectNode request = (ObjectNode) objectMapper.readTree(requestSnapshot);
        request.put("batchId", batchId.toString());
        request.put("idempotencyKey", batchId + ":" + request.path("cells").toString().hashCode());
        request.put("webhookUrl", callbackUrl());
        request.put("webhookSecret", webhookSecret);
        aiWebClient.post().uri("/quiz-gen/drafts/generate")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request).retrieve().toBodilessEntity()
                .block(Duration.ofMinutes(timeoutMinutes));
    }

    private String callbackUrl() {
        return webhookBaseUrl.replaceAll("/+$", "") + "/api/internal/webhooks/assessment-question-generation";
    }
}
