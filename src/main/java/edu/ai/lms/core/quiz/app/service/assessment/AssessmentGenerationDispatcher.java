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

import edu.ai.lms.config.CorrelationIdFilter;
import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentGenerationDispatcher {
    private final AssessmentWorkflowRepository workflow;
    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper;

    @Value("${quiz.generation.webhook.base-url}")
    private String webhookBaseUrl;
    @Value("${quiz.generation.timeout-minutes:20}")
    private long timeoutMinutes;

    @Async
    public void dispatch(UUID batchId) {
        long startedAt = System.nanoTime();
        log.info("event=assessment.generation.dispatch_started batch_id={}", batchId);
        workflow.markBatchProcessing(batchId);
        try {
            send(batchId, workflow.batchRequestSnapshot(batchId));
            log.info("event=assessment.generation.dispatch_accepted batch_id={} duration_ms={}",
                    batchId, elapsedMillis(startedAt));
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            log.error("event=assessment.generation.dispatch_failed batch_id={} duration_ms={} error_type={}",
                    batchId, elapsedMillis(startedAt), ex.getClass().getSimpleName(), ex);
            workflow.failBatch(batchId, message.substring(0, Math.min(1000, message.length())));
        }
    }

    @Async
    public void retryCell(UUID batchId, String requestSnapshot) {
        long startedAt = System.nanoTime();
        log.info("event=assessment.generation.retry_started batch_id={}", batchId);
        try {
            send(batchId, requestSnapshot);
            log.info("event=assessment.generation.retry_accepted batch_id={} duration_ms={}",
                    batchId, elapsedMillis(startedAt));
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            log.error("event=assessment.generation.retry_failed batch_id={} duration_ms={} error_type={}",
                    batchId, elapsedMillis(startedAt), ex.getClass().getSimpleName(), ex);
            workflow.failBatch(batchId, message.substring(0, Math.min(1000, message.length())));
        }
    }

    private void send(UUID batchId, String requestSnapshot) throws Exception {
        ObjectNode request = (ObjectNode) objectMapper.readTree(requestSnapshot);
        request.put("batchId", batchId.toString());
        request.put("idempotencyKey", batchId + ":" + request.path("cells").toString().hashCode());
        request.put("webhookUrl", callbackUrl());
        int cellCount = request.withArray("cells").size();
        log.info("event=assessment.generation.ai_request batch_id={} cell_count={}", batchId, cellCount);
        aiWebClient.post().uri("/quiz-gen/drafts/generate")
                .header(CorrelationIdFilter.HEADER_NAME, batchId.toString())
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request).retrieve().toBodilessEntity()
                .block(Duration.ofMinutes(timeoutMinutes));
    }

    private String callbackUrl() {
        return webhookBaseUrl.replaceAll("/+$", "") + "/api/internal/webhooks/assessment-question-generation";
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
